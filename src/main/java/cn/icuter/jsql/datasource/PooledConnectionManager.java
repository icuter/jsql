package cn.icuter.jsql.datasource;

import cn.icuter.jsql.pool.PooledObject;
import cn.icuter.jsql.pool.PooledObjectManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author edward
 * @since 2018-08-19
 */
public class PooledConnectionManager implements PooledObjectManager<Connection> {

    private final int checkValidTimeout; // seconds, default 5s
    private int invalidTimeout;          // milliseconds, default -1 set invalid immediately
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;

    PooledConnectionManager(String url, String username, String password) {
        this(url, username, password, null, -1, 5);
    }

    PooledConnectionManager(String url, String username, String password, String driverClassName) {
        this(url, username, password, driverClassName, -1);
    }

    PooledConnectionManager(String url, String username, String password, String driverClassName, int checkValidTimeout) {
        this(url, username, password, driverClassName, -1, checkValidTimeout);
    }

    private PooledConnectionManager(String url, String username, String password, String driverClassName,
                                    int invalidTimeout, int checkValidTimeout) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.invalidTimeout = invalidTimeout;
        this.checkValidTimeout = checkValidTimeout;

        registerDriverClassName();
    }

    private void registerDriverClassName() {
        // maybe set outside
        if (driverClassName == null) {
            return;
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid driver name: " + driverClassName, e);
        }
    }

    @Override
    public PooledObject<Connection> create() throws Exception {
        return new PooledObject<>(newConnection());
    }

    private Connection newConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(true); // in case Driver's auto-commit is false
        return connection;
    }

    @Override
    public void invalid(PooledObject<Connection> pooledObject) throws Exception {
        while (pooledObject.isBorrowed() && !pooledObject.getObject().isClosed()) {
            if (invalidTimeout > 0) {
                long now = System.currentTimeMillis();
                if (now - pooledObject.getLastBorrowedTime() > invalidTimeout) {
                    break;
                }
            } else if (invalidTimeout < 0) {
                break; // set invalid immediately
            }
        }
        if (!pooledObject.getObject().isClosed()) {
            pooledObject.getObject().close();
        }
    }

    @Override
    public boolean validate(PooledObject<Connection> pooledObject) throws Exception {
        try {
            return validateConnection(pooledObject);
        } catch (Exception e) {
            // Catch exception and return false is for pooled object removal
            // TODO do log here
            return false;
        }
    }

    private boolean validateConnection(PooledObject<Connection> pooledObject) throws SQLException {
        return pooledObject != null && !pooledObject.getObject().isClosed() && validateQuery(pooledObject.getObject());
    }

    private boolean validateQuery(Connection connection) throws SQLException {
        return connection.isValid(checkValidTimeout);
    }

    public void setInvalidTimeout(int invalidTimeout) {
        this.invalidTimeout = invalidTimeout;
    }
}
