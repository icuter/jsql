package cn.icuter.jsql.executor;

import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.exception.JSQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author edward
 * @since 2019-03-16
 */
@Ignore
public class JoinTableTest {
    public static final String TABLE_NAME_FIRST = "t_first";
    public static final String TABLE_NAME_SECOND = "t_second";
    public static final String TABLE_NAME_THIRD = "t_third";
    private static JSQLDataSource dataSource;
    private static final int TOTAL_COUNT = 10;

    @BeforeClass
    public static void setup() throws IOException {
        dataSource = TestUtils.getDataSource();
        try {
            dataSource.sql("DROP TABLE " + TABLE_NAME_THIRD).execUpdate();
        } catch (JSQLException e) {
            // ignore
        }
        try {
            dataSource.sql("DROP TABLE " + TABLE_NAME_SECOND).execUpdate();
        } catch (JSQLException e) {
            // ignore
        }
        try {
            dataSource.sql("DROP TABLE " + TABLE_NAME_FIRST).execUpdate();
        } catch (JSQLException e) {
            // ignore
        }
        try {
            dataSource.sql(createThirdTableSql()).execUpdate();
            dataSource.sql(createSecondTableSql()).execUpdate();
            dataSource.sql(createFirstTableSql()).execUpdate();

            for (int i = 0; i < TOTAL_COUNT; i++) {
                initRecords();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @AfterClass
    public static void tearDown() throws IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME_FIRST).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME_SECOND).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME_THIRD).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
        } catch (Exception e) {
            // ignore
        } finally {
            executor.close();
            dataSource.close();
            dataSource = null;
        }
    }

    private static String createFirstTableSql() {
        return "CREATE TABLE " + TABLE_NAME_FIRST + "\n" +
                "(\n" +
                "  f_id VARCHAR(60) NOT NULL,\n" +
                "  t_f_col VARCHAR(60),\n" +
//                "  t_col VARCHAR(60),\n" +
                "  s_id VARCHAR(60),\n" +
                "  PRIMARY KEY (f_id), \n" +
                "  CONSTRAINT t_s_id_fk\n" +
                "  FOREIGN KEY (s_id) REFERENCES " + TABLE_NAME_SECOND + " (s_id))";
    }

    private static String createSecondTableSql() {
        return "CREATE TABLE " + TABLE_NAME_SECOND + "\n" +
                "(\n" +
                "  s_id VARCHAR(60) NOT NULL,\n" +
                "  t_s_col VARCHAR(60),\n" +
//                "  t_col VARCHAR(60),\n" +
                "  t_id VARCHAR(60),\n" +
                "  PRIMARY KEY (s_id),\n" +
                "  CONSTRAINT t_t_id_fk\n" +
                "  FOREIGN KEY (t_id) REFERENCES " + TABLE_NAME_THIRD + " (t_id))";
    }
    private static String createThirdTableSql() {
        return "CREATE TABLE " + TABLE_NAME_THIRD + "\n" +
                "(\n" +
                "  t_id VARCHAR(60) NOT NULL,\n" +
                "  t_t_col VARCHAR(60),\n" +
//                "  t_col VARCHAR(60),\n" +
                "  PRIMARY KEY (t_id))";
    }

    private static void initRecords() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getTransactionExecutor();
        try {
            String tId = UUID.randomUUID().toString();
            dataSource.insert(TABLE_NAME_THIRD)
                    .values(
                            Cond.eq("t_id", tId),
//                            Cond.eq("t_col", "t_col_value_third"),
                            Cond.eq("t_t_col", "col_value_" + tId)
                    ).execUpdate(executor);
            String sId = UUID.randomUUID().toString();
            dataSource.insert(TABLE_NAME_SECOND)
                    .values(
                            Cond.eq("s_id", sId),
                            Cond.eq("t_id", tId),
//                            Cond.eq("t_col", "t_col_value_second"),
                            Cond.eq("t_s_col", "col_value_" + sId)
                    ).execUpdate(executor);
            String fId = UUID.randomUUID().toString();
            dataSource.insert(TABLE_NAME_FIRST)
                    .values(
                            Cond.eq("f_id", fId),
                            Cond.eq("s_id", sId),
//                            Cond.eq("t_col", "t_col_value_first"),
                            Cond.eq("t_f_col", "col_value_" + fId)
                    ).execUpdate(executor);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testInnerJoinOn() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .joinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .joinOn(TABLE_NAME_THIRD + " t3", Cond.var("t2.t_id", "t3.t_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .joinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testInnerJoinUsing() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST)
                    .joinUsing(TABLE_NAME_SECOND, "s_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST)
                    .joinUsing(TABLE_NAME_SECOND, "s_id")
                    .joinUsing(TABLE_NAME_THIRD, "t_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testFullJoinOn() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .fullJoinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .fullJoinOn(TABLE_NAME_THIRD + " t3", Cond.var("t2.t_id", "t3.t_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .fullJoinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testFullJoinUsing() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .fullJoinUsing(TABLE_NAME_SECOND, "s_id")
                    .fullJoinUsing(TABLE_NAME_THIRD, "t_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .fullJoinUsing(TABLE_NAME_SECOND,"s_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testLeftJoinOn() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .leftJoinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .leftJoinOn(TABLE_NAME_THIRD + " t3", Cond.var("t2.t_id", "t3.t_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .leftJoinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testLeftJoinUsing() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .leftJoinUsing(TABLE_NAME_SECOND, "s_id")
                    .leftJoinUsing(TABLE_NAME_THIRD, "t_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .leftJoinUsing(TABLE_NAME_SECOND,"s_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testRightJoinOn() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .rightJoinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .rightJoinOn(TABLE_NAME_THIRD + " t3", Cond.var("t2.t_id", "t3.t_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .rightJoinOn(TABLE_NAME_SECOND + " t2", Cond.var("t1.s_id", "t2.s_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
    @Test
    public void testRightJoinUsing() throws JSQLException, IOException {
        JdbcExecutor executor = dataSource.getJdbcExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .rightJoinUsing(TABLE_NAME_SECOND, "s_id")
                    .rightJoinUsing(TABLE_NAME_THIRD, "t_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
            joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST + " t1")
                    .rightJoinUsing(TABLE_NAME_SECOND,"s_id")
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            executor.close();
        }
    }
}
