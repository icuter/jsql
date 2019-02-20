package cn.icuter.jsql.executor;

import cn.icuter.jsql.ORMTable;
import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.data.JSQLBlob;
import cn.icuter.jsql.data.JSQLClob;
import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.datasource.JdbcExecutorPool;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.orm.ORMapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author edward
 * @since 2019-02-12
 */
public class ORMTest {
    public static final String TABLE_NAME = "t_orm_test";
    private static JSQLDataSource dataSource;
    private static JdbcExecutorPool pool;

    @BeforeClass
    public static void setup() throws IOException {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setMaxPoolSize(3);
        poolConfiguration.setCreateRetryCount(3);
        dataSource = TestUtils.getDataSource();
        pool = dataSource.createExecutorPool(poolConfiguration);
        try (JdbcExecutor executor = pool.getExecutor()) {
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            dataSource.sql(TestUtils.getCreateOrmTableSql()).execUpdate(executor);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try (JdbcExecutor jdbcExecutor = pool.getExecutor()) {
            dataSource.sql("DROP table " + TABLE_NAME).execUpdate(jdbcExecutor);
        } finally {
            pool.close();
            dataSource = null;
            pool = null;
        }
    }

    @Test
    public void testORM() throws Exception {
        ORMTable ormTable = createOrmTable();

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
            ormTable.setfClob("hello clob update");
            ormTable.setfString("hello string update");
            ormTable.setfBlobObj(null);
            ormTable.setfClobObj(null);
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

    @Test
    public void testLob() throws Exception {
        try (JdbcExecutor jdbcExecutor = pool.getExecutor()) {
            ORMTable ormTable = createOrmTable();

            dataSource.insert(TABLE_NAME).values(ormTable).execUpdate(jdbcExecutor);

            List<ORMTable> ormTableList = dataSource.select().from(TABLE_NAME)
                    .where().eq("orm_id", ormTable.getOrmId())
                    .execQuery(jdbcExecutor, ORMTable.class);

            Assert.assertEquals(1, ormTableList.size());

            ORMTable ormTableSelect = ormTableList.get(0);
            Blob blobSelected = ormTableSelect.getfBlobObj();
            byte[] blobBytes = blobSelected.getBytes(1L, (int) blobSelected.length());

            Assert.assertArrayEquals(ormTableSelect.getfBlob(), blobBytes);
            Assert.assertArrayEquals(ormTable.getfBlobObj().getBytes(1L, (int) ormTable.getfBlobObj().length()), blobBytes);

            Clob clobSelected = ormTableSelect.getfClobObj();
            String clobString = clobSelected.getSubString(1L, (int) clobSelected.length());

            Assert.assertEquals(ormTableSelect.getfClob(), clobString);
            Assert.assertEquals(ormTable.getfClobObj().getSubString(1L, (int) ormTable.getfClobObj().length()), clobString);

        }
    }

    @Test
    public void testORMapper() throws Exception {
        ORMTable ormTable = createOrmTable();
        ORMapper<ORMTable> mapper = ORMapper.of(ormTable);
        Map<String, Object> mapAttrs = mapper.toMapIgnoreNullValue();
        Assert.assertEquals(ormTable.getOrmId(), mapAttrs.get("orm_id"));
        Assert.assertEquals(ormTable.getfInt(), mapAttrs.get("f_int"));
        Assert.assertEquals(ormTable.getfInteger(), mapAttrs.get("f_integer"));
        Assert.assertEquals(ormTable.getfString(), mapAttrs.get("f_string"));
        Assert.assertEquals(ormTable.getfDouble(), mapAttrs.get("f_double"));
        Assert.assertEquals(ormTable.getfDecimal(), mapAttrs.get("f_decimal"));
        String ormTableClob = ormTable.getfClobObj().getSubString(1L, (int) ormTable.getfClobObj().length());
        JSQLClob clob = (JSQLClob) mapAttrs.get("f_clob");
        String clobString = clob.getSubString(1L, (int) clob.length());
        Assert.assertEquals(ormTableClob, clobString);
        byte[] ormTableBytes = ormTable.getfBlobObj().getBytes(1L, (int) ormTable.getfBlobObj().length());
        JSQLBlob blob = (JSQLBlob) mapAttrs.get("f_blob");
        Assert.assertArrayEquals(ormTableBytes, blob.getBytes(1L, (int) blob.length()));

        ormTable.setfInteger(null);
        ormTable.setfDecimal(null);
        ormTable.setfString(null);
        mapper = ORMapper.of(ormTable);
        mapAttrs = mapper.toMapIgnoreNullValue();
        Assert.assertFalse(mapAttrs.containsKey("f_integer"));
        Assert.assertFalse(mapAttrs.containsKey("f_decimal"));
        Assert.assertFalse(mapAttrs.containsKey("f_string"));

        mapper = ORMapper.of(ormTable);
        mapAttrs = mapper.toMap();
        Assert.assertTrue(mapAttrs.containsKey("f_integer"));
        Assert.assertTrue(mapAttrs.containsKey("f_decimal"));
        Assert.assertTrue(mapAttrs.containsKey("f_string"));
    }

    private ORMTable createOrmTable() {
        ORMTable ormTable = new ORMTable();
        ormTable.setOrmId(UUID.randomUUID().toString());
        ormTable.setfBlobObj(dataSource.createBlob("created from JSQL Blob".getBytes()));
        ormTable.setfClobObj(dataSource.createClob("created from JSQL Clob"));
        ormTable.setfDecimal(new BigDecimal("10.002"));
        ormTable.setfInt(100);
        ormTable.setfInteger(100);
        ormTable.setfDouble(10.2d);
        ormTable.setfString("hello string");
        return ormTable;
    }
}
