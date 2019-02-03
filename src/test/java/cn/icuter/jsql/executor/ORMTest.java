package cn.icuter.jsql.executor;

import cn.icuter.jsql.BaseDataSourceTest;
import cn.icuter.jsql.ORMTable;
import cn.icuter.jsql.datasource.JdbcExecutorPool;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.util.List;
import java.util.UUID;

/**
 * @author edward
 * @since 2019-02-02
 */
@FixMethodOrder
public class ORMTest extends BaseDataSourceTest {
    private static final String TABLE_NAME = "t_orm_test";
    private static JdbcExecutorPool pool;

    @BeforeClass
    public static void setup() throws IOException {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setMaxPoolSize(3);
        pool = dataSource.createExecutorPool(poolConfiguration);
        try (JdbcExecutor executor = pool.getExecutor()) {
            dataSource.sql("CREATE TABLE " + TABLE_NAME + "\n" +
                    "(\n" +
                    "  orm_id VARCHAR(60) NOT NULL,\n" +
                    "  f_blob BLOB NULL,\n" +
                    "  f_string VARCHAR(60) NULL,\n" +
                    "  f_int INTEGER NULL,\n" +
                    "  f_integer INTEGER NULL,\n" +
                    "  f_double decimal(10,3) NULL,\n" +
                    "  f_decimal decimal(10,3) NULL,\n" +
                    "  f_clob TEXT NULL,\n" + // mysql TEXT oracle CLOB
                    "  PRIMARY KEY (orm_id))").execUpdate(executor);
        } catch (JSQLException e) {
            throw new IOException(e);
        }
    }

    @Test
    public void testORM() throws Exception {
        ORMTable ormTable = new ORMTable();
        ormTable.setOrmId(UUID.randomUUID().toString());
        ormTable.setfBlob("hello blob".getBytes());
        ormTable.setfClob("hello clob");
        ormTable.setfDecimal(new BigDecimal("10.002"));
        ormTable.setfInt(100);
        ormTable.setfInteger(100);
        ormTable.setfDouble(10.2d);
        ormTable.setfString("hello string");
        TransactionExecutor txExecutor = pool.getTransactionExecutor();
        try {
            dataSource.insert(TABLE_NAME).values(ormTable).execUpdate(txExecutor);
            txExecutor.commit();
        } catch (Exception e) {
            txExecutor.rollback();
            throw e;
        } finally {
            txExecutor.close();
        }

        txExecutor = pool.getTransactionExecutor();
        try {
            ormTable.setfBlob("hello blob update".getBytes());
            ormTable.setfString("hello string update");
            dataSource.update(TABLE_NAME).set(ormTable).where().eq("orm_id", ormTable.getOrmId()).execUpdate(txExecutor);
            txExecutor.commit();
        } catch (Exception e) {
            txExecutor.rollback();
            throw e;
        } finally {
            txExecutor.close();
        }
        try (JdbcExecutor jdbcExecutor = pool.getExecutor()) {
            List<ORMTable> ormTableList = dataSource
                    .select()
                    .from(TABLE_NAME)
                    .where()
                    .eq("orm_id", ormTable.getOrmId())
                    .execQuery(jdbcExecutor, ORMTable.class);
            Assert.assertEquals(1, ormTableList.size());

            ORMTable ormTableFromSelect = ormTableList.get(0);

            Assert.assertEquals(ormTable.getfInt(), ormTableFromSelect.getfInt());
            Assert.assertEquals(ormTable.getfInteger(), ormTableFromSelect.getfInteger());
            Assert.assertEquals(ormTable.getfDecimal(), ormTableFromSelect.getfDecimal());
            Assert.assertEquals(ormTable.getfDouble(), ormTableFromSelect.getfDouble(), 0);
            Assert.assertEquals(ormTable.getfString(), ormTableFromSelect.getfString());
            Assert.assertArrayEquals(ormTable.getfBlob(), ormTableFromSelect.getfBlob());
            Assert.assertEquals(ormTable.getfClob(), ormTableFromSelect.getfClob());
            Assert.assertEquals(ormTable.getOrmId(), ormTableFromSelect.getOrmId());

            Blob fBlob = ormTableFromSelect.getfBlobObj();
            Assert.assertArrayEquals(ormTable.getfBlob(), fBlob.getBytes(1, (int) fBlob.length()));

            Clob fClob = ormTableFromSelect.getfClobObj();
            Assert.assertEquals(ormTable.getfClob(), fClob.getSubString(1, (int) fClob.length()));

            int deleted = dataSource.delete().from(TABLE_NAME).where().eq("orm_id", ormTable.getOrmId()).execUpdate(jdbcExecutor);

            Assert.assertEquals(1, deleted);
        }
    }

    /*@Test
    public void testLob() throws Exception {
        try (JdbcExecutor jdbcExecutor = pool.getExecutor()) {
            ORMTable ormTable = new ORMTable();
            ormTable.setOrmId(UUID.randomUUID().toString());
            ormTable.setfBlobObj();
            ormTable.setfClobObj();
            ormTable.setfDecimal(new BigDecimal("10.002"));
            ormTable.setfInt(100);
            ormTable.setfInteger(100);
            ormTable.setfDouble(10.2d);
            ormTable.setfString("hello string");

            dataSource.insert(TABLE_NAME).values(ormTable).execUpdate(jdbcExecutor);
        }
    }*/

    @AfterClass
    public static void tearDown() throws Exception {
        try (JdbcExecutor jdbcExecutor = pool.getExecutor()) {
            dataSource.sql("DROP table " + TABLE_NAME).execUpdate(jdbcExecutor);
        } finally {
            pool.close();
        }
    }
}
