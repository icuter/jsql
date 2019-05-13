package cn.icuter.jsql.datasource;

import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.pool.PooledObject;
import cn.icuter.jsql.pool.PooledObjectManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author edward
 * @since 2018-08-19
 */
public class PooledConnectionManager implements PooledObjectManager<Connection> {

    private static final JSQLLogger LOGGER = Logs.getLogger(PooledConnectionManager.class);

    private int checkValidTimeout; // seconds, default 5s
    private int invalidTimeout;    // milliseconds, default -1 set invalid immediately
    private JSQLDataSource dataSource;

    PooledConnectionManager(JSQLDataSource dataSource) {
        this(dataSource, -1, 5);
    }

    PooledConnectionManager(JSQLDataSource dataSource, int invalidTimeout, int checkValidTimeout) {
        this.dataSource = dataSource;
        this.invalidTimeout = invalidTimeout;
        this.checkValidTimeout = checkValidTimeout;
    }

    @Override
    public PooledObject<Connection> create() throws JSQLException {
        try {
            PooledConnection pooledConnection = new PooledConnection(newConnection());

            LOGGER.trace("pooled object was created");
            LOGGER.trace("created pooled connection object detail: " + pooledConnection.pooledObject);

            return pooledConnection.pooledObject;
        } catch (SQLException e) {
            throw new PoolException("creating connection error", e);
        }
    }

    protected Connection newConnection() throws SQLException {
        try {
            return dataSource.createConnection();
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void invalid(PooledObject<Connection> pooledObject) throws JSQLException {
        Connection connection = getRawConnection(pooledObject);
        try {
            while (pooledObject.isBorrowed() && !connection.isClosed()) {
                if (invalidTimeout > 0) {
                    long now = System.currentTimeMillis();
                    if (now - pooledObject.getLastBorrowedTime() > invalidTimeout) {
                        LOGGER.debug("Connection is closing after waited " + invalidTimeout + "ms");
                        break;
                    }
                } else {
                    // set invalid immediately
                    LOGGER.debug("Connection is closing immediately");
                    break;
                }
            }
            if (!connection.isClosed()) {
                connection.close();
            }
            LOGGER.debug("Connection of pooled object was closed, it's detail: " + pooledObject);
        } catch (SQLException e) {
            throw new PoolException("invaliding pooled object error, pooled detail: " + pooledObject.toString(), e);
        }
    }

    @Override
    public boolean validate(PooledObject<Connection> pooledObject) throws JSQLException {
        try {
            return validateConnection(pooledObject);
        } catch (Exception e) {
            // Catch exception and return false is for pooled object removal
            LOGGER.error("Connection is invalid and return false for pooled object removal", e);
            return false;
        }
    }

    private boolean validateConnection(PooledObject<Connection> pooledObject) throws SQLException {
        if (pooledObject == null) {
            return false;
        }
        Connection connection = getRawConnection(pooledObject);
        return !connection.isClosed() && validateQuery(connection);
    }

    private boolean validateQuery(Connection connection) throws SQLException {
        Dialect dialect = dataSource.getDialect();
        if (dialect.supportConnectionIsValid()) {
            return connection.isValid(checkValidTimeout);
        } else if (dialect.validationSql() != null && dialect.validationSql().length() > 0) {
            try (Statement s = connection.createStatement()) {
                s.setQueryTimeout(checkValidTimeout); // doesn't work while network error
                try (ResultSet resultSet = s.executeQuery(dialect.validationSql())) {
                    return resultSet.next();
                }
            }
        }
        LOGGER.info("ignore validating query for " + dialect.getDialectName());
        return true;
    }

    private Connection getRawConnection(PooledObject<Connection> pooledObject) {
        Connection connection = pooledObject.getObject();
        if (connection instanceof PooledConnection) {
            return ((PooledConnection) connection).connection;
        }
        return connection;
    }

    public void setInvalidTimeout(int invalidTimeout) {
        this.invalidTimeout = invalidTimeout;
    }

    public void setCheckValidTimeout(int checkValidTimeout) {
        this.checkValidTimeout = checkValidTimeout;
    }
}
