package cn.icuter.jsql.datasource;

import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
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

    private static final JSQLLogger LOGGER = Logs.getLogger(PooledConnectionManager.class);

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

        LOGGER.debug(String.format("pooled connection detail, username: %s,"
                        + " password: %s, invalidTimeout: %d, checkValidTimeout: %d, driverClassName: %s",
                username, "***", invalidTimeout, checkValidTimeout, driverClassName));
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
    public PooledObject<Connection> create() throws JSQLException {
        try {
            PooledObject<Connection> pooledObject = new PooledObject<>(newConnection());

            LOGGER.trace("pooled object was created");
            LOGGER.trace("created pooled object detail: " + pooledObject);

            return pooledObject;
        } catch (SQLException e) {
            throw new PoolException("creating connection error", e);
        }
    }

    private Connection newConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(true); // in case Driver's auto-commit is false
        return connection;
    }

    @Override
    public void invalid(PooledObject<Connection> pooledObject) throws JSQLException {
        LOGGER.trace("invalidating pooled object");
        try {
            while (pooledObject.isBorrowed() && !pooledObject.getObject().isClosed()) {
                if (invalidTimeout > 0) {
                    long now = System.currentTimeMillis();
                    if (now - pooledObject.getLastBorrowedTime() > invalidTimeout) {
                        LOGGER.debug("pooled object was invalidated waited " + invalidTimeout + "ms");
                        break;
                    }
                } else if (invalidTimeout < 0) {
                    // set invalid immediately
                    LOGGER.debug("pooled object was invalidated immediately");
                    break;
                }
            }
            if (!pooledObject.getObject().isClosed()) {
                pooledObject.getObject().close();
            }
            LOGGER.debug("invalidated pooled object detail: " + pooledObject);
        } catch (SQLException e) {
            throw new PoolException("invaliding pooled object error, pooled detail: " + pooledObject.toString(), e);
        }
    }

    @Override
    public boolean validate(PooledObject<Connection> pooledObject) throws JSQLException {
        try {
            boolean validation = validateConnection(pooledObject);

            LOGGER.debug("validation pooled object detail: " + pooledObject);

            return validation;
        } catch (Exception e) {
            // Catch exception and return false is for pooled object removal
            LOGGER.error("Connection is invalid and return false for pooled object removal", e);
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
