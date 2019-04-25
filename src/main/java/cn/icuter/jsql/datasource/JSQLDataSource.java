package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.DeleteBuilder;
import cn.icuter.jsql.builder.InsertBuilder;
import cn.icuter.jsql.builder.SQLBuilder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.builder.UnionSelectBuilder;
import cn.icuter.jsql.builder.UpdateBuilder;
import cn.icuter.jsql.data.JSQLBlob;
import cn.icuter.jsql.data.JSQLClob;
import cn.icuter.jsql.data.JSQLNClob;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.dialect.UnknownDialect;
import cn.icuter.jsql.exception.DataSourceException;
import cn.icuter.jsql.executor.CloseableJdbcExecutor;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.executor.TransactionExecutor;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.pool.DefaultObjectPool;
import cn.icuter.jsql.pool.ObjectPool;
import cn.icuter.jsql.pool.PooledObjectManager;
import cn.icuter.jsql.util.ObjectUtil;

import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author edward
 * @since 2018-08-10
 */
public class JSQLDataSource implements javax.sql.ConnectionPoolDataSource {

    private static final JSQLLogger LOGGER = Logs.getLogger(JSQLDataSource.class);

    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Dialect dialect;   // parse by url or name or dialect class name
    private int loginTimeout;  // default 5s
    private ConnectionPool connectionPool;
    private JdbcExecutorPool executorPool;

    /**
     * <pre>
     * jdbc.properties
     * - url             mandatory
     * - username        mandatory
     * - password        mandatory
     * - driverClass     optional if dialect set
     * - dialect         optional if driverClass set
     * - loginTimeout    optional default 5s
     * - pool.maxPoolSize             default 20
     * - pool.idleTimeout             default 30 minutes
     * - pool.validateOnBorrow        default true
     * - pool.validateOnReturn        default false
     * - pool.pollTimeout             default 10 seconds
     * - pool.createRetryCount        default 0
     * - pool.scheduledThreadLifeTime default 5 minutes
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
                    throw new IllegalArgumentException("unsupported dialect for [" + dialectInProp + ']', e);
                }
            }
        } else {
            dialect = Dialects.parseUrl(jdbcProp.getProperty("url"));
        }
        int loginTimeout = Integer.valueOf(jdbcProp.getProperty("loginTimeout", "5"));
        init(jdbcProp.getProperty("url"), jdbcProp.getProperty("username"), jdbcProp.getProperty("password"),
                jdbcProp.getProperty("driverClass"), loginTimeout, dialect);
        initPool(jdbcProp);
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
        initPool(new Properties());
    }

    private void init(String url, String username, String password, String driverClassName, int loginTimeout, Dialect dialect) {
        ObjectUtil.requireNonEmpty(url, "url must not be empty");

        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.dialect = dialect;
        if (dialect.requireUserPassword()) {
            ObjectUtil.requireNonEmpty(username, "username must not be empty");
            ObjectUtil.requireNonNull(password, "password must not be null");
        }
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
            LOGGER.debug("Connection login timeout has set globally with " + this.loginTimeout + "s");

            DriverManager.setLoginTimeout(this.loginTimeout);
        }
        LOGGER.debug("set up " + toString());
    }

    private void initPool(Properties poolProp) {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        if (poolProp.containsKey("pool.maxPoolSize")) {
            poolConfiguration.setMaxPoolSize(Integer.valueOf(poolProp.getProperty("pool.maxPoolSize")));
        }
        if (poolProp.containsKey("pool.idleTimeout")) {
            poolConfiguration.setIdleTimeout(Long.valueOf(poolProp.getProperty("pool.idleTimeout")));
        }
        if (poolProp.containsKey("pool.validateOnBorrow")) {
            poolConfiguration.setValidateOnBorrow(Boolean.valueOf(poolProp.getProperty("pool.validateOnBorrow")));
        }
        if (poolProp.containsKey("pool.validateOnReturn")) {
            poolConfiguration.setValidateOnReturn(Boolean.valueOf(poolProp.getProperty("pool.validateOnReturn")));
        }
        if (poolProp.containsKey("pool.pollTimeout")) {
            poolConfiguration.setPollTimeout(Long.valueOf(poolProp.getProperty("pool.pollTimeout")));
        }
        if (poolProp.containsKey("pool.createRetryCount")) {
            poolConfiguration.setCreateRetryCount(Integer.valueOf(poolProp.getProperty("pool.createRetryCount")));
        }
        if (poolProp.containsKey("pool.scheduledThreadLifeTime")) {
            poolConfiguration.setScheduledThreadLifeTime(Long.valueOf(poolProp.getProperty("pool.scheduledThreadLifeTime")));
        }
        ObjectPool<Connection> objectPool = createConnectionObjectPool(poolConfiguration);
        connectionPool = new ConnectionPool(objectPool);
        executorPool = new JdbcExecutorPool(objectPool);
    }

    public TransactionExecutor createTransaction() {
        return new TransactionExecutor(createConnection(false));
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
            LOGGER.error("creating Connection error for " + url, e);
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
        PooledObjectManager<Connection> manager = new PooledConnectionManager(this);
        return poolConfiguration == null ? new DefaultObjectPool<>(manager)
                : new DefaultObjectPool<>(manager, poolConfiguration);
    }

    public JdbcExecutorPool createExecutorPool() {
        return createExecutorPool(null);
    }

    public JdbcExecutorPool createExecutorPool(PoolConfiguration poolConfiguration) {
        return new JdbcExecutorPool(createConnectionObjectPool(poolConfiguration));
    }

    public Connection getConnection() {
        return connectionPool.getConnection();
    }

    public JdbcExecutor getJdbcExecutor() {
        return executorPool.getExecutor();
    }
    public void close() {
        connectionPool.close();
    }
    public TransactionExecutor getTransactionExecutor() {
        return executorPool.getTransactionExecutor();
    }

    protected JdbcExecutor provideExecutor() {
        return getJdbcExecutor();
    }

    public Builder select(String... cols) {
        return new ExecutableSelectBuilder(dialect).select(cols);
    }
    public Builder update(String table) {
        return new ExecutableUpdateBuilder(dialect).update(table);
    }
    public Builder insert(String table) {
        return new ExecutableInsertBuilder(dialect).insert(table);
    }
    public Builder delete() {
        return new ExecutableDeleteBuilder(dialect).delete();
    }
    public Builder sql(String sql, Object... values) {
        return new ExecutableSQLBuilder().sql(sql).value(values);
    }

    public Builder union(Builder... builders) {
        return new ExecutableUnionSelectBuilder(dialect, false, builders);
    }
    public Builder unionAll(Builder... builders) {
        return new ExecutableUnionSelectBuilder(dialect, true, builders);
    }
    public Builder union(Collection<Builder> builders) {
        return new ExecutableUnionSelectBuilder(dialect, false, builders);
    }
    public Builder unionAll(Collection<Builder> builders) {
        return new ExecutableUnionSelectBuilder(dialect, true, builders);
    }

    public Clob createClob(String initData) {
        return new JSQLClob(initData);
    }

    public NClob createNClob(String initData) {
        return new JSQLNClob(initData);
    }

    public Blob createBlob(byte[] initData) {
        return new JSQLBlob(initData);
    }

    public Clob createClob() {
        return new JSQLClob();
    }

    public NClob createNClob() {
        return new JSQLNClob();
    }

    public Blob createBlob() {
        return new JSQLBlob();
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }
    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return createConnectionPool();
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return null;
    }

    class ExecutableSelectBuilder extends SelectBuilder {
        ExecutableSelectBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableUpdateBuilder extends UpdateBuilder {
        ExecutableUpdateBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableInsertBuilder extends InsertBuilder {
        ExecutableInsertBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableDeleteBuilder extends DeleteBuilder {
        ExecutableDeleteBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableSQLBuilder extends SQLBuilder {
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableUnionSelectBuilder extends UnionSelectBuilder {
        ExecutableUnionSelectBuilder(Dialect dialect, boolean isUnionAll, Builder... builders) {
            super(dialect, isUnionAll, builders);
        }
        ExecutableUnionSelectBuilder(Dialect dialect, boolean isUnionAll, Collection<Builder> builders) {
            super(dialect, isUnionAll, builders);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("JSQLDataSource{")
                .append("url='").append(url).append('\'')
                .append(", username='").append(username).append('\'')
                .append(", password='***'")
                .append(", driverClassName='").append(driverClassName).append('\'')
                .append(", dialect=").append(dialect.getDialectName())
                .append(", loginTimeout=").append(loginTimeout).append("s")
                .append('}').toString();
    }
}
