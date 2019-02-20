package cn.icuter.jsql.executor;

import cn.icuter.jsql.TestTable;
import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.datasource.ConnectionPool;
import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.datasource.JdbcExecutorPool;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.exception.ExecutionException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.transaction.Transaction;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author edward
 * @since 2019-02-12
 */
public class JdbcExecutorTest {
    public static final String TABLE_NAME = "t_jsql_test";
    private static JdbcExecutorPool pool;
    private static JSQLDataSource dataSource;
    private static ConnectionPool poolConn;
    private int order = 0;

    @BeforeClass
    public static void setup() throws IOException {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setCreateRetryCount(3);
        poolConfiguration.setMaxPoolSize(3);
        dataSource = TestUtils.getDataSource();
        pool = dataSource.createExecutorPool(poolConfiguration);
        poolConn = dataSource.createConnectionPool(poolConfiguration);
        try (JdbcExecutor executor = pool.getExecutor()) {
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            dataSource.sql(TestUtils.getCreateJdbcTableSql()).execUpdate(executor);
        } catch (JSQLException e) {
            throw new IOException(e);
        }
    }

    @AfterClass
    public static void tearDown() throws JSQLException, IOException {
        try (JdbcExecutor executor = pool.getExecutor()) {
            dataSource.sql("DROP TABLE " + TABLE_NAME).execUpdate(executor);
        } finally {
            pool.close();
            poolConn.close();
            dataSource = null;
            pool = null;
            poolConn = null;
        }
    }

    @Test
    public void testDefaultExecutor() throws Exception {
        try (JdbcExecutor executor = pool.getExecutor()) {
            testExecutorBase(executor);
        }
    }

    @Test
    public void testTransactionExecutor() throws Exception {
        TransactionExecutor executor = pool.getTransactionExecutor();
        try {
            testExecutorBase(executor);
        } finally {
            pool.returnExecutor(executor);
        }
        Assert.assertTrue(executor.wasCommitted());
        Assert.assertFalse(executor.wasRolledBack());
    }

    @Test
    public void testCommit() throws Exception {
        TransactionExecutor executor = pool.getTransactionExecutor();
        try {
            insertTestRecord(executor);
            insertTestRecord(executor);

        } catch (JSQLException e) {
            executor.rollback();
        } finally {
            pool.returnExecutor(executor);
        }
        Assert.assertTrue(executor.wasCommitted());
        Assert.assertFalse(executor.wasRolledBack());

        try (JdbcExecutor jdbcExecutor = pool.getExecutor()) {
            List<Map<String, Object>> list = dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
            Assert.assertEquals(list.size(), 2);

            int delCnt = dataSource.delete().from(TABLE_NAME).execUpdate(jdbcExecutor);
            Assert.assertEquals(delCnt, 2);
        }
    }

    @Test
    public void testRollback() throws Exception {
        TransactionExecutor executor = pool.getTransactionExecutor();
        try {
            insertTestRecord(executor);
            insertTestRecord(executor);

            executor.rollback();
        } finally {
            pool.returnExecutor(executor);
        }
        Assert.assertFalse(executor.wasCommitted());
        Assert.assertTrue(executor.wasRolledBack());

        JdbcExecutor jdbcExecutor = pool.getExecutor();
        try {
            List<Map<String, Object>> list = dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
            Assert.assertTrue(list.isEmpty());
        } finally {
            pool.returnExecutor(jdbcExecutor);
        }
    }

    @Test
    public void testRollbackSavepoint() throws Exception {
        Dialect dialect = dataSource.getDialect();
        if (!dialect.supportSavepoint()) {
            // cubrid jdbc driver do not support savepoint
            return;
        }
        TransactionExecutor executor = pool.getTransactionExecutor();
        String testId = null;
        try {
            testId = insertTestRecord(executor).getTestId();

            executor.addSavepoint("savepoint_1");

            dataSource.insert(TABLE_NAME)
                    .values(Cond.eq("test_id", testId),
                            Cond.eq("t_col_1", "tx-val-2-2-1"),
                            Cond.eq("t_col_2", "tx-val-2-2-2")
                    )
                    .execUpdate(executor);

            executor.addSavepoint("savepoint_2");
        } catch (JSQLException e) {
            executor.rollback("savepoint_1");

            Assert.assertTrue(executor.getState() == Transaction.State.ROLLBACK_SAVEPOINT);
        } finally {
            pool.returnExecutor(executor);
        }
        Assert.assertTrue(executor.wasCommitted());
        Assert.assertFalse(executor.wasRolledBack());

        executor = pool.getTransactionExecutor();
        try {
            List<TestTable> testTableList = dataSource.select().from(TABLE_NAME).execQuery(executor, TestTable.class);
            Assert.assertEquals(testTableList.size(), 1);

            int cnt = dataSource.delete().from(TABLE_NAME).where().eq("test_id", testId).execUpdate(executor);
            Assert.assertEquals(cnt, 1);
        } finally {
            pool.returnExecutor(executor);
        }
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testBuilderOffsetLimit() throws Exception {
        if (dataSource.getDialect() == Dialects.SQLITE) {
            // SQLite Driver DO NOT support cursor operation
            return;
        }
        JdbcExecutor jdbcExecutor = pool.getExecutor();
        try {
            for (int i = 0; i < 10; i++) {
                insertTestRecord(jdbcExecutor);
            }
            int offset = 1;
            int limit = 5;
            List<Map<String, Object>> resultMapInDialect = dataSource.select().from(TABLE_NAME).orderBy("order_num desc")
                    .offset(offset).limit(limit)
                    .execQuery(jdbcExecutor);

            List<Map<String, Object>> resultMapWithoutDialect = new SelectBuilder()
                    .select().from(TABLE_NAME).orderBy("order_num desc")
                    .offset(offset).limit(limit)
                    .execQuery(jdbcExecutor);

            assertEquals(resultMapInDialect.size(), resultMapWithoutDialect.size());

            for (int i = 0; i < resultMapInDialect.size(); i++) {
                Map<String, Object> mapInDialect = resultMapInDialect.get(i);
                Map<String, Object> mapWithoutDialect = resultMapWithoutDialect.get(i);

                assertEquals(mapInDialect.get("test_id"), mapWithoutDialect.get("test_id"));
                assertEquals(mapInDialect.get("order_num"), mapWithoutDialect.get("order_num"));
            }
            // for update
            if (dataSource.getDialect() != Dialects.DB2) {
                resultMapInDialect = dataSource.select().from(TABLE_NAME).orderBy("order_num desc")
                        .offset(offset).limit(limit)
                        .execQuery(jdbcExecutor);

                resultMapWithoutDialect = new SelectBuilder()
                        .select().from(TABLE_NAME).orderBy("order_num desc")
                        .offset(offset).limit(limit)
                        .execQuery(jdbcExecutor);

                assertEquals(resultMapInDialect.size(), resultMapWithoutDialect.size());

                for (int i = 0; i < resultMapInDialect.size(); i++) {
                    Map<String, Object> mapInDialect = resultMapInDialect.get(i);
                    Map<String, Object> mapWithoutDialect = resultMapWithoutDialect.get(i);

                    assertEquals(mapInDialect.get("test_id"), mapWithoutDialect.get("test_id"));
                    assertEquals(mapInDialect.get("order_num"), mapWithoutDialect.get("order_num"));
                }
            }
            // offset start from 0
            offset = 0;
            resultMapInDialect = dataSource.select().from(TABLE_NAME).orderBy("order_num desc")
                    .offset(offset).limit(limit)
                    .execQuery(jdbcExecutor);

            resultMapWithoutDialect = new SelectBuilder()
                    .select().from(TABLE_NAME).orderBy("order_num desc")
                    .offset(offset).limit(limit)
                    .execQuery(jdbcExecutor);

            assertEquals(resultMapInDialect.size(), resultMapWithoutDialect.size());

            for (int i = 0; i < resultMapInDialect.size(); i++) {
                Map<String, Object> mapInDialect = resultMapInDialect.get(i);
                Map<String, Object> mapWithoutDialect = resultMapWithoutDialect.get(i);

                assertEquals(mapInDialect.get("test_id"), mapWithoutDialect.get("test_id"));
                assertEquals(mapInDialect.get("order_num"), mapWithoutDialect.get("order_num"));
            }
            dataSource.delete().from(TABLE_NAME).execUpdate(jdbcExecutor);
        } finally {
            pool.returnExecutor(jdbcExecutor);
        }
    }

    @Test
    public void testUnionSelect() throws Exception {
        JdbcExecutor jdbcExecutor = pool.getExecutor();
        List<TestTable> testTableList = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            testTableList.add(insertTestRecord(jdbcExecutor));
        }
        testTableList.sort((Comparator<Object>) (o1, o2) -> {
            TestTable t1 = (TestTable) o1;
            TestTable t2 = (TestTable) o2;
            int t1OrderNum = t1.getOrderNum();// != null ? t1.getOrderNum() : 0;
            int t2OrderNum = t2.getOrderNum();// != null ? t2.getOrderNum() : 0;
            return t2OrderNum - t1OrderNum;
        });
        try {
            List<TestTable> unionResult = dataSource.unionAll(
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(0).limit(3).orderBy("order_num desc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(3).limit(3).orderBy("order_num desc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(6).limit(4).orderBy("order_num desc").build()
            ).orderBy("order_num desc").execQuery(jdbcExecutor, TestTable.class);

            assertEquals(testTableList.size(), unionResult.size());

            for (int i = 0; i < testTableList.size(); i++) {
                TestTable testTable = testTableList.get(i);
                assertEquals(testTable.getOrderNum(), unionResult.get(i).getOrderNum());
                assertEquals(testTable.getTestId(), unionResult.get(i).getTestId());
            }

            Collections.reverse(testTableList);

            unionResult = dataSource.unionAll(
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(0).limit(3).orderBy("order_num asc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(3).limit(3).orderBy("order_num asc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(6).limit(4).orderBy("order_num asc").build()
            ).orderBy("order_num asc").execQuery(jdbcExecutor, TestTable.class);

            assertEquals(testTableList.size(), unionResult.size());

            for (int i = 0; i < testTableList.size(); i++) {
                TestTable testTable = testTableList.get(i);
                assertEquals(testTable.getOrderNum(), unionResult.get(i).getOrderNum());
                assertEquals(testTable.getTestId(), unionResult.get(i).getTestId());
            }

            int offset = 3;
            int limit = 5;
            unionResult = dataSource.unionAll(
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(0).limit(3).orderBy("order_num asc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(3).limit(3).orderBy("order_num asc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(6).limit(4).orderBy("order_num asc").build()
            ).offset(offset).limit(limit).orderBy("order_num asc").execQuery(jdbcExecutor, TestTable.class);
            for (int i = offset; i < limit + offset; i++) {
                TestTable testTable = testTableList.get(i);
                assertEquals(testTable.getOrderNum(), unionResult.get(i - offset).getOrderNum());
                assertEquals(testTable.getTestId(), unionResult.get(i - offset).getTestId());
            }
            offset = 0;
            unionResult = dataSource.unionAll(
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(0).limit(3).orderBy("order_num asc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(3).limit(3).orderBy("order_num asc").build(),
                    dataSource.select("test_id", "t_col_1", "t_col_2", "order_num").from(TABLE_NAME).offset(6).limit(4).orderBy("order_num asc").build()
            ).offset(offset).limit(limit).orderBy("order_num asc").execQuery(jdbcExecutor, TestTable.class);
            for (int i = offset; i < limit + offset; i++) {
                TestTable testTable = testTableList.get(i);
                assertEquals(testTable.getOrderNum(), unionResult.get(i - offset).getOrderNum());
                assertEquals(testTable.getTestId(), unionResult.get(i - offset).getTestId());
            }

            dataSource.delete().from(TABLE_NAME).execUpdate(jdbcExecutor);
        } finally {
            pool.returnExecutor(jdbcExecutor);
        }
    }

    @Test
    public void testCreateTransaction() throws Exception {
        TestTable testTable;
        try (TransactionExecutor txExecutor = dataSource.createTransaction()) {
            testTable = insertTestRecord(txExecutor);
            txExecutor.commit();
        }
        try (TransactionExecutor txExecutorEnd = dataSource.createTransaction()) {
            int cnt = dataSource
                    .update(TABLE_NAME).set(Cond.eq("t_col_1", testTable.getCol1() + "_updated"))
                    .where().eq("test_id", testTable.getTestId())
                    .execUpdate(txExecutorEnd);
            Assert.assertEquals(cnt, 1);
            txExecutorEnd.commit();
        }
        try (TransactionExecutor txExecutorClose = dataSource.createTransaction()) {
            int cnt = dataSource
                    .delete().from(TABLE_NAME).where().eq("test_id", testTable.getTestId())
                    .execUpdate(txExecutorClose);
            Assert.assertEquals(cnt, 1);
            txExecutorClose.commit();
        }
    }

    @Test
    public void testBatchUpdate() throws Exception {
        List<Builder> batchList = new LinkedList<>();
        batchList.add(newInsertBuilder());
        batchList.add(newInsertBuilder());
        batchList.add(newInsertBuilder());
        batchList.add(newInsertBuilder());
        try (TransactionExecutor txExecutor = dataSource.createTransaction()) {
            txExecutor.execBatch(batchList);
            txExecutor.commit();
        }
        try (JdbcExecutor jdbcExecutor = dataSource.createJdbcExecutor()) {
            int cnt = dataSource.delete().from(TABLE_NAME).execUpdate(jdbcExecutor);
            Assert.assertEquals(cnt, batchList.size());
        }
    }

    @Test
    public void testMultipleBatchUpdate() throws Exception {
        List<Builder> batchList = new LinkedList<>();
        int size = 10;
        for (int i = 0; i < size; i++) {
            TestTable testTable = createTestTableRecord();
            Builder insert = dataSource.insert(TABLE_NAME).values(testTable).build();
            batchList.add(insert);

            Builder updateBuilder = dataSource.update(TABLE_NAME)
                    .set(Cond.eq("t_col_1", testTable.getCol1() + "_updated"))
                    .where().eq("test_id", testTable.getTestId()).build();
            batchList.add(updateBuilder);

            Builder deleteBuilder = dataSource.delete().from(TABLE_NAME)
                    .where().eq("test_id", testTable.getTestId()).build();
            batchList.add(deleteBuilder);
        }
        try (TransactionExecutor jdbcExecutor = dataSource.createTransaction()) {
            jdbcExecutor.execBatch(batchList);
            jdbcExecutor.commit();
        }
    }

    @Test(expected = ExecutionException.class)
    public void testBatchUpdateException() throws Exception {
        List<Builder> batchList = new LinkedList<>();
        batchList.add(newInsertBuilder());
        batchList.add(newInsertBuilder());

        TestTable testTable = createTestTableRecord();
        testTable.setTestId(UUID.randomUUID().toString());
        batchList.add(dataSource.insert(TABLE_NAME).values(testTable).build());
        batchList.add(dataSource.insert(TABLE_NAME).values(Cond.eq("test_id", testTable.getTestId())).build());
        batchList.add(newInsertBuilder());
        batchList.add(newInsertBuilder());
        TransactionExecutor txExecutor = dataSource.createTransaction();
        try {
            txExecutor.execBatch(batchList);
        } catch (JSQLException e) {
            txExecutor.rollback();
            throw e;
        } finally {
            txExecutor.close();
        }
    }

    @Test
    public void testPooledConnectionSelect() throws SQLException, JSQLException {
        for (int i = 0; i < 5; i++) {
            Connection connection = poolConn.getConnection();
            JdbcExecutor jdbcExecutor = new DefaultJdbcExecutor(connection);
            dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
            connection.close();
        }
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorConnectionException() throws Exception {
        TransactionExecutor txExecutor = dataSource.createTransaction();
        try {
            dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
            txExecutor.commit();
        } finally {
            txExecutor.close();
        }
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorCommitException() throws Exception {
        TransactionExecutor txExecutor = dataSource.createTransaction();
        try {
            dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
            txExecutor.commit();
        } finally {
            txExecutor.close();
        }
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorRollbackException() throws Exception {
        TransactionExecutor txExecutor = dataSource.createTransaction();
        try {
            dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
            txExecutor.rollback();
        } finally {
            txExecutor.close();
        }
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testExceptionAfterReturned() throws Exception {
        Connection connection = poolConn.getConnection();
        JdbcExecutor jdbcExecutor = new DefaultJdbcExecutor(connection);
        dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
        poolConn.returnConnection(connection);
        dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testExceptionAfterClosed() throws Exception {
        Connection connection = poolConn.getConnection();
        JdbcExecutor jdbcExecutor = new DefaultJdbcExecutor(connection);
        dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
        connection.close();
        dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testJdbcExecutorException() throws JSQLException {
        JdbcExecutor jdbcExecutor = pool.getExecutor();
        try {
            dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
        } finally {
            pool.returnExecutor(jdbcExecutor);
        }
        dataSource.select().from(TABLE_NAME).execQuery(jdbcExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorException() throws JSQLException {
        TransactionExecutor txExecutor = pool.getTransactionExecutor();
        try {
            dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
        } finally {
            pool.returnExecutor(txExecutor);
        }
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
    }

    private void testExecutorBase(JdbcExecutor executor) throws JSQLException {
        TestTable testTable = insertTestRecord(executor);

        List<Map<String, Object>> result = dataSource.select("count(1) as cnt")
                .from(TABLE_NAME)
                .where().eq("test_id", testTable.getTestId()).execQuery(executor);
        Map<String, Object> resultMap = result.get(0);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(String.valueOf(resultMap.get("cnt")), "1");

        result = dataSource.select("test_id", "t_col_1", "t_col_2")
                .from(TABLE_NAME)
                .where().eq("test_id", testTable.getTestId()).execQuery(executor);
        resultMap = result.get(0);
        Assert.assertEquals(resultMap.get("test_id"), testTable.getTestId());
        Assert.assertEquals(resultMap.get("t_col_1"), testTable.getCol1());
        Assert.assertEquals(resultMap.get("t_col_2"), testTable.getCol2());

        TestTable testTableUpdate = new TestTable();
        testTableUpdate.setCol1("col1_update");
        dataSource.update(TABLE_NAME).set(testTableUpdate).execUpdate(executor);

        List<TestTable> testTableList = dataSource.select().from(TABLE_NAME)
                .where().eq("test_id", testTable.getTestId()).execQuery(executor, TestTable.class);
        Assert.assertEquals(testTableList.size(), 1);

        TestTable tableBySelect = testTableList.get(0);
        Assert.assertEquals(tableBySelect.getCol1(), testTableUpdate.getCol1());
        Assert.assertEquals(tableBySelect.getCol2(), testTable.getCol2());

        List<Map<String,Object>> emptyResult = dataSource.select().from(TABLE_NAME)
                .where().eq("test_id", "xxxxxxxxxyy(*&12").execQuery(executor);
        Assert.assertEquals(emptyResult.size(), 0);

        int successCnt = dataSource.delete().from(TABLE_NAME).where().eq("test_id", testTable.getTestId()).execUpdate(executor);
        Assert.assertEquals(successCnt, 1);
    }

    private TestTable insertTestRecord(JdbcExecutor executor) throws JSQLException {
        TestTable testTable = createTestTableRecord();
        dataSource.insert(TABLE_NAME).values(testTable).execUpdate(executor);
        return testTable;
    }

    private Builder newInsertBuilder() {
        TestTable testTable = createTestTableRecord();
        return dataSource.insert(TABLE_NAME).values(testTable).build();
    }

    private TestTable createTestTableRecord() {
        TestTable testTable = new TestTable();
        testTable.setTestId(UUID.randomUUID().toString());
        testTable.setCol1("col1");
        testTable.setCol2("col2");
        testTable.setOrderNum(order++);
        return testTable;
    }
}
