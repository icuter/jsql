package cn.icuter.jsql.executor;

import cn.icuter.jsql.BaseDataSourceTest;
import cn.icuter.jsql.TestTable;
import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.datasource.JdbcExecutorPool;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.ExecutionException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.TransactionCommitException;
import cn.icuter.jsql.exception.TransactionRollbackExcetpion;
import cn.icuter.jsql.transaction.Transaction;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author edward
 * @since 2018-09-22
 */
@FixMethodOrder
public class JdbcExecutorTest extends BaseDataSourceTest {

    private static final String TABLE_NAME = "t_jsql_test";
    static JdbcExecutorPool pool;

    @BeforeClass
    public static void setup() throws JSQLException, IOException {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setMaxPoolSize(1);
        pool = dataSource.createExecutorPool(poolConfiguration);
        try (JdbcExecutor executor = pool.getExecutor()) {
            dataSource.sql("CREATE TABLE " + TABLE_NAME + "\n" +
                    "(\n" +
                    "  test_id VARCHAR(60) PRIMARY KEY,\n" +
                    "  t_col_1 VARCHAR(60) NULL,\n" +
                    "  t_col_2 VARCHAR(60) NULL\n" +
                    ")").execUpdate(executor);
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
        TransactionExecutor executor = pool.getTransactionExecutor();
        String testId = null;
        try {
            testId = insertTestRecord(executor).getTestId();

            executor.addSavepoint("savepoint_1");

            dataSource.insert(TABLE_NAME)
                    .values(Cond.eq("test_id", "0123456789012345678901234567890123456789012345678901234567890123456789"),
                            Cond.eq("t_col_1", "tx-val-2-2-2"),
                            Cond.eq("t_col_2", "tx-val-2-2-2"))
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

    @Test
    public void testBuilderOffsetLimit() throws Exception {
        JdbcExecutor jdbcExecutor = pool.getExecutor();
        try {
            for (int i = 0; i < 10; i++) {
                insertTestRecord(jdbcExecutor);
            }
            List<Map<String, Object>> resultMapInDialect = dataSource.select().from(TABLE_NAME).orderBy("test_id desc")
                    .offset(1)
                    .limit(10)
                    .execQuery(jdbcExecutor);

            List<Map<String, Object>> resultMapWithoutDialect = new SelectBuilder() {{
                select().from(TABLE_NAME).orderBy("test_id desc").offset(1).limit(10).build();
            }}.execQuery(jdbcExecutor);

            assertEquals(resultMapInDialect.size(), resultMapWithoutDialect.size());

            for (int i = 0; i < resultMapInDialect.size(); i++) {
                Map<String, Object> mapInDialect = resultMapInDialect.get(i);
                Map<String, Object> mapWithoutDialect = resultMapWithoutDialect.get(i);

                assertEquals(mapInDialect.get("test_id"), mapWithoutDialect.get("test_id"));
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
        }
        TransactionExecutor txExecutorEnd = dataSource.createTransaction();
        try {
            int cnt = dataSource
                    .update(TABLE_NAME).set(Cond.eq("t_col_1", testTable.getCol1() + "_updated"))
                    .where().eq("test_id", testTable.getTestId())
                    .execUpdate(txExecutorEnd);
            Assert.assertEquals(cnt, 1);
        } finally {
            txExecutorEnd.end();
        }
        TransactionExecutor txExecutorClose = dataSource.createTransaction();
        try {
            int cnt = dataSource
                    .delete().from(TABLE_NAME).where().eq("test_id", testTable.getTestId())
                    .execUpdate(txExecutorClose);
            Assert.assertEquals(cnt, 1);
        } finally {
            txExecutorClose.close();
        }
    }

    @Test
    public void testBatchUpdate() throws Exception {
        List<Builder> batchList = new LinkedList<Builder>() {{
            add(newInsertBuilder());
            add(newInsertBuilder());
            add(newInsertBuilder());
            add(newInsertBuilder());
        }};
        try (TransactionExecutor txExecutor = dataSource.createTransaction()) {
            txExecutor.execBatch(batchList);
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
        try (JdbcExecutor jdbcExecutor = dataSource.createTransaction()) {
            jdbcExecutor.execBatch(batchList);
        }
    }

    @Test(expected = ExecutionException.class)
    public void testBatchUpdateException() throws Exception {
        List<Builder> batchList = new LinkedList<Builder>() {{
            add(newInsertBuilder());
            add(newInsertBuilder());

            TestTable testTable = createTestTableRecord();
            testTable.setCol1("01234567890123456789012345678901234567890123456789012345678901234567891");
            add(dataSource.insert(TABLE_NAME).values(testTable).build());

            add(newInsertBuilder());
            add(newInsertBuilder());
        }};
        TransactionExecutor txExecutor = dataSource.createTransaction();
        try {
            txExecutor.execBatch(batchList);
        } catch (JSQLException e) {
            txExecutor.rollback();
            throw e;
        } finally {
            txExecutor.end();
        }
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionEndedException() throws Exception {
        try (TransactionExecutor txExecutorRollback = dataSource.createTransaction()) {
            TestTable testTable1 = insertTestRecord(txExecutorRollback);

            txExecutorRollback.rollback();

            dataSource.select().from(TABLE_NAME).where().eq("test_id", testTable1.getTestId()).execQuery(txExecutorRollback);
        }
    }

    @Test(expected = TransactionCommitException.class)
    public void testTransactionEndedOperationException() throws Exception {
        try (TransactionExecutor txExecutorRollback = dataSource.createTransaction()) {
            insertTestRecord(txExecutorRollback);

            txExecutorRollback.rollback();
            txExecutorRollback.commit();
        }
    }

    @Test(expected = TransactionRollbackExcetpion.class)
    public void testTransactionEndedOperationException2() throws Exception {
        try (TransactionExecutor txExecutorRollback = dataSource.createTransaction()) {
            dataSource.select().from(TABLE_NAME).execQuery(txExecutorRollback);

            txExecutorRollback.commit();
            txExecutorRollback.rollback();
        }
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorConnectionException() throws Exception {
        JdbcExecutor txExecutor = dataSource.createTransaction();
        try {
            dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
        } finally {
            txExecutor.close();
        }
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorCommitException() throws Exception {
        TransactionExecutor txExecutor = dataSource.createTransaction();
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
        txExecutor.commit();

        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
    }

    @Test(expected = ExecutionException.class)
    public void testTransactionExecutorRollbackException() throws Exception {
        TransactionExecutor txExecutor = dataSource.createTransaction();
        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
        txExecutor.rollback();

        dataSource.select().from(TABLE_NAME).execQuery(txExecutor);
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

        List<Map<String, Object>> result = dataSource.select("*", "count(1) as cnt").from(TABLE_NAME)
                .where().eq("test_id", testTable.getTestId()).execQuery(executor);
        Map<String, Object> resultMap = result.get(0);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(resultMap.get("cnt"), 1L);
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
        return testTable;
    }

    @AfterClass
    public static void tearDown() throws JSQLException, IOException {
        try (JdbcExecutor executor = pool.getExecutor()) {
            dataSource.sql("DROP TABLE " + TABLE_NAME).execUpdate(executor);
        }
        pool.close();
    }

}
