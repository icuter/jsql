package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.DeleteBuilder;
import cn.icuter.jsql.builder.InsertBuilder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.builder.UpdateBuilder;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.dialect.UnknownDialect;
import cn.icuter.jsql.exception.DataSourceException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.CloseableJdbcExecutor;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.executor.TransactionExecutor;
import cn.icuter.jsql.pool.DefaultObjectPool;
import cn.icuter.jsql.pool.ObjectPool;
import cn.icuter.jsql.pool.PooledObjectManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * @author edward
 * @since 2018-08-10
 */
public class JSQLDataSource {

    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Dialect dialect;   // parse by url or name or dialect class name
    private int loginTimeout;  // default 5s

    /**
     * <pre>
     * jdbc.properties
     * - url             mandatory
     * - username        mandatory
     * - password        mandatory
     * - driverClass     optional if dialect set
     * - dialect         optional if driverClass set
     * - loginTimeout    optional default 5s
     * </pre>
     * @param jdbcProp jdbc properties in file or create by manually
     */
    public JSQLDataSource(final Properties jdbcProp) {
        String dialectInProp = jdbcProp.getProperty("dialect");
        if (dialectInProp != null) {
            dialect = Dialects.parseName(dialectInProp);
            // dialect name or dialectClass not found
            if (dialect == null || dialect instanceof UnknownDialect) {
                try {
                    // try to create an custom dialect class
                    dialect = (Dialect) Class.forName(dialectInProp).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unsupport dialect for [" + dialectInProp + ']', e);
                }
            }
        } else {
            dialect = Dialects.parseUrl(jdbcProp.getProperty("url"));
        }
        int loginTimeout = Integer.valueOf(jdbcProp.getProperty("loginTimeout", "5"));
        init(jdbcProp.getProperty("url"), jdbcProp.getProperty("username"), jdbcProp.getProperty("password"),
                jdbcProp.getProperty("driverClass"), loginTimeout, dialect);
    }

    public JSQLDataSource(String url, String username, String password) {
        this(url, username, password, null, Dialects.parseUrl(url));
    }

    public JSQLDataSource(String url, String username, String password, Dialect dialect) {
        this(url, username, password, null, dialect);
    }

    public JSQLDataSource(String url, String username, String password, String driverClassName) {
        this(url, username, password, driverClassName, Dialects.parseUrl(url));
    }

    public JSQLDataSource(String url, String username, String password, String driverClassName, Dialect dialect) {
        init(url, username, password, driverClassName, 5, dialect);
    }

    private void init(String url, String username, String password, String driverClassName, int loginTimeout, Dialect dialect) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.dialect = dialect;
        if ((this.driverClassName == null || this.driverClassName.length() <= 0) && this.dialect != null) {
            this.driverClassName = this.dialect.getDriverClassName();
        }
        try {
            Objects.requireNonNull(this.driverClassName, "Driver Class Name must not be null!");
            Class.forName(this.driverClassName);
        } catch (ClassNotFoundException e) {
            throw new DataSourceException("initializing driver class error", e);
        }
        this.loginTimeout = loginTimeout;
        if (DriverManager.getLoginTimeout() <= 0 && this.loginTimeout > 0) {
            DriverManager.setLoginTimeout(this.loginTimeout);
        }
    }

    public TransactionExecutor createTransaction() {
        Connection connection = createConnection(false);
        TransactionExecutor executor = new TransactionExecutor(connection) {
            @Override
            public void close() throws IOException {
                if (!wasCommitted() && !wasRolledBack()) {
                    rollback();
                }
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    throw new IOException("closing Connection in transaction error", e);
                }
            }
        };
        executor.setStateListener((transaction, state) -> {
            try {
                if (transaction.wasCommitted() || transaction.wasRolledBack()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new DataSourceException("closing Connection in StateListener error", e);
            }
        });
        return executor;
    }

    public JdbcExecutor createJdbcExecutor() {
        return new CloseableJdbcExecutor(createConnection());
    }

    public Connection createConnection() {
        return createConnection(true);
    }

    public Connection createConnection(boolean autoCommit) {
        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(autoCommit);
            return connection;
        } catch (SQLException e) {
            throw new DataSourceException("creating Connection error for " + url, e);
        }
    }

    public ConnectionPool createConnectionPool() {
        return createConnectionPool(null);
    }

    public ConnectionPool createConnectionPool(PoolConfiguration poolConfiguration) {
        return new ConnectionPool(createConnectionObjectPool(poolConfiguration));
    }

    private ObjectPool<Connection> createConnectionObjectPool(PoolConfiguration poolConfiguration) {
        PooledObjectManager<Connection> manager = new PooledConnectionManager(url, username, password);
        return poolConfiguration == null ? new DefaultObjectPool<>(manager)
                : new DefaultObjectPool<>(manager, poolConfiguration);
    }

    public JdbcExecutorPool createExecutorPool() {
        return createExecutorPool(null);
    }

    public JdbcExecutorPool createExecutorPool(PoolConfiguration poolConfiguration) {
        return new JdbcExecutorPool(createConnectionObjectPool(poolConfiguration));
    }

    public SelectBuilder selectBuilder() {
        return new SelectBuilder(dialect);
    }
    public UpdateBuilder updateBuilder() {
        return new UpdateBuilder(dialect);
    }
    public InsertBuilder insertBuilder() {
        return new InsertBuilder(dialect);
    }
    public DeleteBuilder deleteBuilder() {
        return new DeleteBuilder(dialect);
    }

    @Override
    public String toString() {
        return new StringBuilder("JSQLDataSource{")
                .append("url='").append(url).append('\'')
                .append(", username='").append(username).append('\'')
                .append(", password='").append(password).append('\'')
                .append(", driverClassName='").append(driverClassName).append('\'')
                .append(", dialect=").append(dialect).append(", loginTimeout=").append(loginTimeout)
                .append('}').toString();
    }
}
