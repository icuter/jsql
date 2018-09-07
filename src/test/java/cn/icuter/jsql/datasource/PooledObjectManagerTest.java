package cn.icuter.jsql.datasource;

import cn.icuter.jsql.pool.PooledObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;

/**
 * @author edward
 * @since 2018-08-29
 */
public class PooledObjectManagerTest {

    private static PooledConnectionManager manager;

    @BeforeClass
    public static void setup() {
        manager = new PooledConnectionManager(
                "jdbc:mysql://xt5:3308/cmxt?serverTimezone=GMT%2B8",
                "coremail", "3020509829", "com.mysql.cj.jdbc.Driver", "select 1 from dual");
    }

    @Test
    public void test() throws Exception {
        PooledObject<Connection> pooledObject = manager.create();

        boolean isValid = manager.validate(pooledObject);

        manager.invalid(pooledObject);
    }

}
