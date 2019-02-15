package cn.icuter.jsql.executor;

import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.base.ORMBaseTest;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;

import java.io.IOException;

/**
 * @author edward
 * @since 2019-02-02
 */
@FixMethodOrder
public class ORMMariaDBTest extends ORMBaseTest {

    @BeforeClass
    public static void setup() throws IOException {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setMaxPoolSize(3);
        dataSource = TestUtils.getDataSource("mariadb");
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
                    "  f_clob TEXT NULL,\n" + // mysql TEXT instead of CLOB
                    "  PRIMARY KEY (orm_id))").execUpdate(executor);
        } catch (JSQLException e) {
            throw new IOException(e);
        }
    }
}
