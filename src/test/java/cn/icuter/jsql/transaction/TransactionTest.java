package cn.icuter.jsql.transaction;

import cn.icuter.jsql.builder.InsertBuilder;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.executor.TransactionExecutor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author edward
 * @since 2018-09-16
 */
@FixMethodOrder
public class TransactionTest {

    private static JSQLDataSource dataSource;

    @BeforeClass
    public static void setup() throws IOException {
        Properties properties = new Properties();
        String icuterHome = System.getenv("ICUTER_HOME"); // only for test
        try (InputStream in = new FileInputStream(new File(icuterHome, "conf/jdbc.properties"))) {
            properties.load(in);
            dataSource = new JSQLDataSource(properties);
        }
    }

    @Test
    public void testCommit() {
        TransactionExecutor executor = dataSource.createTransaction();
        try {
            executor.execUpdate(new InsertBuilder() {{
                insertInto("t_jsql_test").values(
                        Cond.eq("t_col_1", "transaction-val-1"),
                        Cond.eq("t_col_2", "transaction-val-1")
                ).build();
            }});
            executor.execUpdate(new InsertBuilder() {{
                insertInto("t_jsql_test").values(
                        Cond.eq("t_col_1", "transaction-val-2"),
                        Cond.eq("t_col_2", "transaction-val-2")
                ).build();
            }});
            executor.commit();
        } catch (Exception e) {
            e.printStackTrace();
            executor.rollback();
        }
        Assert.assertTrue(executor.wasCommitted());
        Assert.assertFalse(executor.wasRolledBack());
        Assert.assertEquals(executor.getState(), Transaction.State.COMMIT);
    }

    @Test
    public void testRollback() {
        TransactionExecutor executor = dataSource.createTransaction();
        try {
            executor.execUpdate(new InsertBuilder() {{
                insertInto("t_jsql_test").values(
                        Cond.eq("t_col_1", "transaction-val-1-1"),
                        Cond.eq("t_col_2", "transaction-val-1-1")
                ).build();
            }});
            executor.execUpdate(new InsertBuilder() {{
                insertInto("t_jsql_test").values(
                        Cond.eq("id", "error_id"),
                        Cond.eq("t_col_1", "transaction-val-2-2"),
                        Cond.eq("t_col_2", "transaction-val-2-2")
                ).build();
            }});
            executor.commit();
        } catch (Exception e) {
            e.printStackTrace();
            executor.rollback();
        }
        Assert.assertFalse(executor.wasCommitted());
        Assert.assertTrue(executor.wasRolledBack());
        Assert.assertEquals(executor.getState(), Transaction.State.ROLLBACK);
    }

    @Test
    public void testRollbackSavepoint() {
        TransactionExecutor executor = dataSource.createTransaction();
        try {
            executor.execUpdate(new InsertBuilder() {{
                insertInto("t_jsql_test").values(
                        Cond.eq("t_col_1", "transaction-val-1-1-1"),
                        Cond.eq("t_col_2", "transaction-val-1-1-1")
                ).build();
            }});
            executor.addSavepoint("savepoint_1");
            executor.execUpdate(new InsertBuilder() {{
                insertInto("t_jsql_test").values(
                        Cond.eq("id", "error_id"),
                        Cond.eq("t_col_1", "transaction-val-2-2-2"),
                        Cond.eq("t_col_2", "transaction-val-2-2-2")
                ).build();
            }});
            executor.commit();
        } catch (Exception e) {
            e.printStackTrace();
            executor.rollback("savepoint_1");
            executor.commit();
        }
        Assert.assertTrue(executor.wasCommitted());
        Assert.assertFalse(executor.wasRolledBack());
    }
}
