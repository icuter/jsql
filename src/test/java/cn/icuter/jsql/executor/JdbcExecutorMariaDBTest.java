package cn.icuter.jsql.executor;

import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.base.JdbcExecutorBaseTest;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;

import java.io.IOException;

/**
 * @author edward
 * @since 2018-09-22
 */
@FixMethodOrder
public class JdbcExecutorMariaDBTest extends JdbcExecutorBaseTest {

    @BeforeClass
    public static void setup() throws IOException {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setMaxPoolSize(3);
        dataSource = TestUtils.getDataSource("mariadb");
        pool = dataSource.createExecutorPool(poolConfiguration);
        poolConn = dataSource.createConnectionPool(poolConfiguration);
        try (JdbcExecutor executor = pool.getExecutor()) {
            dataSource.sql("CREATE TABLE " + TABLE_NAME + "\n" +
                    "(\n" +
                    "  test_id VARCHAR(60) NOT NULL,\n" +
                    "  t_col_1 VARCHAR(60) NULL,\n" +
                    "  t_col_2 VARCHAR(60) NULL,\n" +
                    "  order_num INTEGER NULL,\n" +
                    "  PRIMARY KEY (test_id))").execUpdate(executor);
        } catch (JSQLException e) {
            throw new IOException(e);
        }
    }
}
