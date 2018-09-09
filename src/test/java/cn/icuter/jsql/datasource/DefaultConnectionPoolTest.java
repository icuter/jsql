package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.column.OrgUnit;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.executor.DefaultJdbcExecutor;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.pool.ObjectPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author edward
 * @since 2018-08-15
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultConnectionPoolTest {

    static ObjectPool<Connection> pool;
    int threadSize = 50;

    @BeforeClass
    public static void setup() throws Exception {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setIdleCheckInterval(200L);
//        poolConfiguration.setPollTimeout(500);

        Properties properties = new Properties();
        String icuterHome = System.getenv("ICUTER_HOME"); // only for test
        try (InputStream in = new FileInputStream(new File(icuterHome, "conf/jdbc.properties"))) {
            properties.load(in);
            JSQLDataSource dataSource = new JSQLDataSource(properties);
            pool = dataSource.createConnectionPool(poolConfiguration);
        }
    }

    @Test
    public void testLoop() throws Exception {
        TestingThread[] testingThread = new TestingThread[threadSize];
        for (int i=0; i < testingThread.length; i++) {
            testingThread[i] = new TestingThread();
            testingThread[i].start();
        }
        for (TestingThread t : testingThread) {
            t.join();
        }
    }

    @Test
    public void testBuilder() throws Exception {
        Connection connection = null;
        try {
            connection = pool.borrowObject();
            testSelectBuilder(connection);
        } finally {
            if (connection != null) {
                pool.returnObject(connection);
            }
        }
    }

    // TODO test
    @Test
    public void testBuilderOffsetLimit() throws Exception {
        Connection connection = null;
        try {
            connection = pool.borrowObject();

            Builder builder = new SelectBuilder(Dialects.MYSQL) {{
                select().from("td_org_unit t").orderBy("org_unit_id desc").offset(1).limit(10).build();
            }};
            DefaultJdbcExecutor jdbcExecutor = new DefaultJdbcExecutor(connection);
            jdbcExecutor.setColumnLowerCase(true);
            List<Map<String, Object>> resultMap = jdbcExecutor.execQuery(builder);

            builder = new SelectBuilder() {{
                select().from("td_org_unit t").orderBy("org_unit_id desc").offset(1).limit(10).build();
            }};
            List<Map<String, Object>> resultMap2 = jdbcExecutor.execQuery(builder);
            for (int i = 0; i < resultMap.size(); i++) {
                Map<String, Object> map = resultMap.get(i);
                Map<String, Object> map2 = resultMap2.get(i);

                assertEquals(map.get("org_unit_id"), map2.get("org_unit_id"));
            }
        } finally {
            if (connection != null) {
                pool.returnObject(connection);
            }
        }
    }

    private void testSelectBuilder(Connection connection) throws Exception {
        Builder builder = new SelectBuilder() {{
            select("t.org_ID", "t.org_unit_id as ORG_UNIT_ID",
                    "t.parent_org_unit_id as PARENT_ORG_UNIT_ID", "t.org_unit_name as ORG_UNIT_NAME",
                    "t.org_unit_list_rank as ORG_UNIT_LIST_RANK")
                    .from("td_org_unit t")
                    .orderBy("org_unit_id", "org_id desc")
                    .build();
        }};
        JdbcExecutor jdbcExecutor = new DefaultJdbcExecutor(connection);
        List<Map<String, Object>> resultMap = jdbcExecutor.execQuery(builder);
        List<OrgUnit> resultORM = jdbcExecutor.execQuery(builder, OrgUnit.class);

        System.out.println("resultMap: " + resultMap);
        System.out.println("result: " + resultORM);
    }

    @Test
    public void testLoopClose() throws Exception {
        TestingThread[] testingThread = new TestingThread[threadSize];
        for (int i=0; i < testingThread.length; i++) {
            testingThread[i] = new TestingThread();
            testingThread[i].start();
            if (i == 20) {
                pool.close();
            }
        }
        for (TestingThread t : testingThread) {
            t.join();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        pool.close();
    }

    class TestingThread extends Thread {
        @Override
        public void run() {
            try {
                Connection conn = pool.borrowObject();
                testSelectBuilder(conn);
                Thread.sleep(150L);
                pool.returnObject(conn);
                System.out.println(pool.debugInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}