package cn.icuter.jsql.datasource;

import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.pool.PooledObject;
import cn.icuter.jsql.pool.PooledObjectManager;

import java.sql.Connection;

/**
 * @author edward
 * @since 2018-08-26
 */
public class PooledExecutorManager implements PooledObjectManager<JdbcExecutor> {

    private final PooledConnectionManager pooledConnectionManager;

    public PooledExecutorManager(String url, String username, String password, String driverClassName) {
        pooledConnectionManager = new PooledConnectionManager(url, username, password, driverClassName);
    }

    public PooledExecutorManager(String url, String username, String password, String driverClassName, int checkValidTimeout) {
        pooledConnectionManager = new PooledConnectionManager(url, username, password, driverClassName, checkValidTimeout);
    }

    @Override
    public PooledObject<JdbcExecutor> create() throws Exception {
        PooledObject<Connection> pooledConnection = pooledConnectionManager.create();
        return new PooledObject<>(new PooledJdbcExecutor(pooledConnection.getObject()));
    }

    @Override
    public void invalid(PooledObject<JdbcExecutor> pooledObject) throws Exception {
        pooledConnectionManager.invalid((PooledJdbcExecutor) pooledObject.getObject());
    }

    @Override
    public boolean validate(PooledObject<JdbcExecutor> pooledObject) throws Exception {
        return pooledConnectionManager.validate((PooledJdbcExecutor) pooledObject.getObject());
    }
}
