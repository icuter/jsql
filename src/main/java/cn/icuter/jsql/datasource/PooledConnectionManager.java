package cn.icuter.jsql.datasource;

import cn.icuter.jsql.pool.PooledObject;
import cn.icuter.jsql.pool.PooledObjectManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * @author edward
 * @since 2018-08-19
 */
public class PooledConnectionManager implements PooledObjectManager<Connection> {

    private final int loginTimeout;      // seconds, default 5s
    private final int checkValidTimeout; // seconds, default 5s
    private int invalidTimeout;          // milliseconds, default -1 set invalid immediately
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;

    PooledConnectionManager(String url, String username, String password, String driverClassName) {
        this(url, username, password, driverClassName, 5, -1, 5);
    }

    PooledConnectionManager(String url, String username, String password, String driverClassName, int checkValidTimeout) {
        this(url, username, password, driverClassName, 5, -1, checkValidTimeout);
    }

    private PooledConnectionManager(String url, String username, String password, String driverClassName,
                                    int loginTimeout, int invalidTimeout, int checkValidTimeout) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.loginTimeout = loginTimeout;
        this.invalidTimeout = invalidTimeout;
        this.checkValidTimeout = checkValidTimeout;

        registerDriverClassName();
    }

    private void registerDriverClassName() {
        try {
            Objects.requireNonNull(driverClassName, "Driver Class Name must not be null!");
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
        int originLoginTimeout = DriverManager.getLoginTimeout();
        if (loginTimeout > 0) {
            DriverManager.setLoginTimeout(loginTimeout);
        }
        Connection connection = DriverManager.getConnection(url, username, password);
        if (loginTimeout > 0) {
            DriverManager.setLoginTimeout(originLoginTimeout);
        }
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
