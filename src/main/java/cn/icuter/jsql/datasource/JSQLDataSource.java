package cn.icuter.jsql.datasource;

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
import cn.icuter.jsql.transaction.TransactionOperation;
import cn.icuter.jsql.util.ObjectUtil;

import javax.sql.PooledConnection;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * @author edward
 * @since 2018-08-10
 */
public class JSQLDataSource extends AbstractBuilderDataSource implements javax.sql.ConnectionPoolDataSource, Closeable {

    private static final JSQLLogger LOGGER = Logs.getLogger(JSQLDataSource.class);

    static final String PROP_DRIVER_PREFIX = "driver.";
    static final String PROP_USER = "user";
    static final String PROP_PASSWORD = "password";
    static final String PROP_URL = "url";
    static final String PROP_DRIVER_CLASS = "driverClass";
    static final String PROP_DIALECT = "dialect";
    static final String PROP_LOGIN_TIMEOUT = "loginTimeout";
    static final String PROP_POOL_MAX_POOL_SIZE = "pool.maxPoolSize";
    static final String PROP_POOL_IDLE_TIMEOUT = "pool.idleTimeout";
    static final String PROP_POOL_VALIDATE_ON_BORROW = "pool.validateOnBorrow";
    static final String PROP_POOL_VALIDATE_ON_RETURN = "pool.validateOnReturn";
    static final String PROP_POOL_POLL_TIMEOUT = "pool.pollTimeout";
    static final String PROP_POOL_CREATE_RETRY_COUNT = "pool.createRetryCount";
    static final String PROP_POOL_SCHEDULED_THREAD_LIFETIME = "pool.scheduledThreadLifeTime";

    private String url;
    private String driverClassName;

    /** Makes compatible with different Databases such as MySQL / Oracle / DB2 ... */
    private Dialect dialect;

    /**
     * Sets driver connection timeout globally, if negative will be ignored.
     * Default 5s
     */
    private int loginTimeout;

    /** Sets driver connection properties */
    private Properties driverProps = new Properties();
    private ConnectionPool connectionPool;
    private JdbcExecutorPool executorPool;

    protected JSQLDataSource() {
    }

    public static DataSourceBuilder newDataSourceBuilder() {
        return new DataSourceBuilder();
    }

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
     *
     * - driver.user       jdbc username (prior to username)
     * - driver.password   jdbc password (prior to password)
     * - driver.anyThings  jdbc customizing properties for Driver(such as SocketFactory for proxy request)
     * </pre>
     * @param jdbcProp jdbc properties in file or create by manually
     */
    public JSQLDataSource(final Properties jdbcProp) {
        setDialectByProperties(jdbcProp);
        setDriverProps(jdbcProp);
        int loginTimeout = Integer.parseInt(jdbcProp.getProperty(PROP_LOGIN_TIMEOUT, "5"));
        init(jdbcProp.getProperty(PROP_URL), jdbcProp.getProperty("username"), jdbcProp.getProperty(PROP_PASSWORD),
                jdbcProp.getProperty(PROP_DRIVER_CLASS), loginTimeout, dialect);
        initPool(jdbcProp);
    }

    private void setDialectByProperties(Properties jdbcProp) {
        String dialectInProp = jdbcProp.getProperty(PROP_DIALECT);
        if (dialectInProp != null) {
            dialect = Dialects.parseName(dialectInProp);
            // dialect name or dialectClass not found
            if (dialect == null || dialect instanceof UnknownDialect) {
                try {
                    // try to create an custom dialect class
                    dialect = (Dialect) Class.forName(dialectInProp).newInstance();
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("unsupported dialect for [" + dialectInProp + ']', e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("unsupported dialect for [" + dialectInProp + ']', e);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("unsupported dialect for [" + dialectInProp + ']', e);
                }
            }
        } else {
            dialect = Dialects.parseUrl(jdbcProp.getProperty(PROP_URL));
        }
    }

    private void setDriverProps(Properties jdbcProp) {
        Objects.requireNonNull(jdbcProp, "Jdbc Properties must not be null");
        Set<Object> keySet = jdbcProp.keySet();
        for (Object keyObj : keySet) {
            String key = (String) keyObj;
            if (key.startsWith(PROP_DRIVER_PREFIX)) {
                driverProps.put(key.substring(PROP_DRIVER_PREFIX.length()), jdbcProp.getProperty(key));
            }
        }
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
        this(url, username, password, driverClassName, 5, dialect);
    }

    public JSQLDataSource(String url, String username, String password, String driverClassName, int loginTimeout, Dialect dialect) {
        init(url, username, password, driverClassName, loginTimeout, dialect);
        initPool(new Properties());
    }

    private void init(String url, String username, String password, String driverClassName, int loginTimeout, Dialect dialect) {
        ObjectUtil.requireNonEmpty(url, "url must not be empty");

        this.url = url;
        this.driverClassName = driverClassName;
        this.dialect = dialect;
        if (dialect != null && dialect.requireUserPassword()) {
            if (!driverProps.containsKey(PROP_USER)) {
                ObjectUtil.requireNonEmpty(username, "username must not be empty");
            }
            // sometimes empty password may be acceptable
            if (!driverProps.containsKey(PROP_PASSWORD)) {
                ObjectUtil.requireNonNull(password, "password must not be null");
            }
        }
        if ((this.driverClassName == null || this.driverClassName.length() <= 0) && this.dialect != null) {
            this.driverClassName = this.dialect.getDriverClassName();
        }
        try {
            ObjectUtil.requireNonNull(this.driverClassName, "Driver Class Name must not be null!");
            Class.forName(this.driverClassName);
        } catch (ClassNotFoundException e) {
            throw new DataSourceException("initializing driver class error", e);
        }
        this.loginTimeout = loginTimeout;
        if (DriverManager.getLoginTimeout() <= 0 && this.loginTimeout > 0) {
            LOGGER.debug("Connection login timeout has set globally with " + this.loginTimeout + "s");

            DriverManager.setLoginTimeout(this.loginTimeout);
        }
        if (!driverProps.containsKey(PROP_USER) && username != null) {
            driverProps.setProperty(PROP_USER, username);
        }
        if (!driverProps.containsKey(PROP_PASSWORD) && password != null) {
            driverProps.setProperty(PROP_PASSWORD, password);
        }
        LOGGER.debug("set up " + toString());
    }

    private void initPool(Properties poolProp) {
        ObjectPool<Connection> objectPool = createConnectionObjectPool(getPoolConfiguration(poolProp));
        connectionPool = new ConnectionPool(objectPool);
        executorPool = new JdbcExecutorPool(objectPool);
    }

    PoolConfiguration getPoolConfiguration(Properties poolProp) {
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        if (poolProp.containsKey(PROP_POOL_MAX_POOL_SIZE)) {
            poolConfiguration.setMaxPoolSize(Integer.parseInt(poolProp.getProperty(PROP_POOL_MAX_POOL_SIZE)));
        }
        if (poolProp.containsKey(PROP_POOL_IDLE_TIMEOUT)) {
            poolConfiguration.setIdleTimeout(Long.parseLong(poolProp.getProperty(PROP_POOL_IDLE_TIMEOUT)));
        }
        if (poolProp.containsKey(PROP_POOL_VALIDATE_ON_BORROW)) {
            poolConfiguration.setValidateOnBorrow(Boolean.parseBoolean(poolProp.getProperty(PROP_POOL_VALIDATE_ON_BORROW)));
        }
        if (poolProp.containsKey(PROP_POOL_VALIDATE_ON_RETURN)) {
            poolConfiguration.setValidateOnReturn(Boolean.parseBoolean(poolProp.getProperty(PROP_POOL_VALIDATE_ON_RETURN)));
        }
        if (poolProp.containsKey(PROP_POOL_POLL_TIMEOUT)) {
            poolConfiguration.setPollTimeout(Long.parseLong(poolProp.getProperty(PROP_POOL_POLL_TIMEOUT)));
        }
        if (poolProp.containsKey(PROP_POOL_CREATE_RETRY_COUNT)) {
            poolConfiguration.setCreateRetryCount(Integer.parseInt(poolProp.getProperty(PROP_POOL_CREATE_RETRY_COUNT)));
        }
        if (poolProp.containsKey(PROP_POOL_SCHEDULED_THREAD_LIFETIME)) {
            poolConfiguration.setScheduledThreadLifeTime(Long.parseLong(poolProp.getProperty(PROP_POOL_SCHEDULED_THREAD_LIFETIME)));
        }
        return poolConfiguration;
    }

    public void transaction(TransactionOperation operation) throws Exception {
        TransactionDataSource transactionDataSource = null;
        try {
            transactionDataSource = new TransactionDataSource(this);
            operation.doTransaction(transactionDataSource);
        } catch (Exception e) {
            if (transactionDataSource != null) {
                transactionDataSource.rollback();
            }
            LOGGER.info("error occurs while processing transaction and has been rolled back");
            throw e;
        } finally {
            if (transactionDataSource != null) {
                transactionDataSource.close();
            }
        }
    }

    public TransactionDataSource transaction() {
        return new TransactionDataSource(this);
    }

    public TransactionExecutor createTransaction() {
        return new TransactionExecutor(createConnection(false));
    }

    public JdbcExecutorPool getExecutorPool() {
        return executorPool;
    }

    public JdbcExecutor createJdbcExecutor() {
        return new CloseableJdbcExecutor(createConnection());
    }

    public Connection createConnection() {
        return createConnection(true);
    }

    public Connection createConnection(boolean autoCommit) {
        try {
            Connection connection = DriverManager.getConnection(url, driverProps);
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
        return poolConfiguration == null ? new DefaultObjectPool<Connection>(manager)
                : new DefaultObjectPool<Connection>(manager, poolConfiguration);
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
    public void close() throws IOException {
        connectionPool.close();
    }
    public TransactionExecutor getTransactionExecutor() {
        return executorPool.getTransactionExecutor();
    }

    @Override
    public JdbcExecutor provideExecutor() {
        return getJdbcExecutor();
    }
    @Override
    public Dialect provideDialect() {
        return dialect;
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

    public String getDriverClassName() {
        return driverClassName;
    }

    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    public Logger getParentLogger() {
        return null;
    }

    @Override
    public PooledConnection getPooledConnection() {
        return connectionPool;
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Properties getDriverProperties() {
        return driverProps;
    }

    @Override
    public String toString() {
        return "JSQLDataSource {"
                + "url='" + url + '\''
                + ", driverProps='" + displayDriverProps() + '\''
                + ", driverClassName='" + driverClassName + '\''
                + ", dialect='" + dialect.getDialectName() + '\''
                + ", loginTimeout=" + loginTimeout + "s"
                + '}';
    }

    private String displayDriverProps() {
        StringBuilder driverDebugInfo = new StringBuilder();
        for (Object keyObj : driverProps.keySet()) {
            String key = (String) keyObj;
            driverDebugInfo.append(key).append("=");
            if (PROP_PASSWORD.equals(key)) {
                driverDebugInfo.append("******, ");
            } else {
                driverDebugInfo.append(driverProps.getProperty(key)).append(", ");
            }
        }
        return driverDebugInfo.toString().replaceFirst(",\\s*$", "");
    }

    public static class DataSourceBuilder {
        Properties jdbcProperties = new Properties();

        private DataSourceBuilder() {
        }
        public DataSourceBuilder url(String jdbcUrl) {
            jdbcProperties.setProperty(PROP_URL, jdbcUrl);
            return this;
        }
        public DataSourceBuilder user(String user) {
            jdbcProperties.setProperty(PROP_DRIVER_PREFIX + PROP_USER, user);
            return this;
        }
        public DataSourceBuilder password(String password) {
            jdbcProperties.setProperty(PROP_DRIVER_PREFIX + PROP_PASSWORD, password);
            return this;
        }
        public DataSourceBuilder driverClass(String driverClass) {
            jdbcProperties.setProperty(PROP_DRIVER_CLASS, driverClass);
            return this;
        }
        public DataSourceBuilder dialect(String dialect) {
            jdbcProperties.setProperty(PROP_DIALECT, dialect);
            return this;
        }
        public DataSourceBuilder loginTimeout(int loginTimeout) {
            jdbcProperties.setProperty(PROP_LOGIN_TIMEOUT, String.valueOf(loginTimeout));
            return this;
        }
        public DataSourceBuilder addDriverProperties(Properties jdbcProperties) {
            if (jdbcProperties == null) {
                // ignored processing if jdbcProperties is null
                return this;
            }
            for (Map.Entry<Object, Object> entry : jdbcProperties.entrySet()) {
                this.jdbcProperties.put(PROP_DRIVER_PREFIX + entry.getKey(), entry.getValue());
            }
            return this;
        }
        public DataSourceBuilder addProperties(Properties jdbcProperties) {
            if (jdbcProperties == null) {
                // ignored processing if jdbcProperties is null
                return this;
            }
            for (Map.Entry<Object, Object> entry : jdbcProperties.entrySet()) {
                this.jdbcProperties.put(entry.getKey(), entry.getValue());
            }
            return this;
        }
        public DataSourceBuilder addMapProperties(Map<String, String> supplier) {
            if (!supplier.isEmpty()) {
                jdbcProperties.putAll(supplier);
            }
            return this;
        }
        public DataSourceBuilder poolMaxSize(int poolSize) {
            jdbcProperties.setProperty(PROP_POOL_MAX_POOL_SIZE, String.valueOf(poolSize));
            return this;
        }
        public DataSourceBuilder poolIdleTimeout(long poolIdleTimeout) {
            jdbcProperties.setProperty(PROP_POOL_IDLE_TIMEOUT, String.valueOf(poolIdleTimeout));
            return this;
        }
        public DataSourceBuilder poolValidationOnBorrow(boolean poolValidationOnBorrow) {
            jdbcProperties.setProperty(PROP_POOL_VALIDATE_ON_BORROW, String.valueOf(poolValidationOnBorrow));
            return this;
        }
        public DataSourceBuilder poolValidationOnReturn(boolean poolValidationOnReturn) {
            jdbcProperties.setProperty(PROP_POOL_VALIDATE_ON_RETURN, String.valueOf(poolValidationOnReturn));
            return this;
        }
        public DataSourceBuilder poolPollTimeout(long poolPollTimeout) {
            jdbcProperties.setProperty(PROP_POOL_POLL_TIMEOUT, String.valueOf(poolPollTimeout));
            return this;
        }
        public DataSourceBuilder poolObjectCreateRetryCount(int poolObjectCreateRetryCount) {
            jdbcProperties.setProperty(PROP_POOL_CREATE_RETRY_COUNT, String.valueOf(poolObjectCreateRetryCount));
            return this;
        }
        public DataSourceBuilder poolScheduleThreadLifeTime(long poolScheduleThreadLifeTime) {
            jdbcProperties.setProperty(PROP_POOL_SCHEDULED_THREAD_LIFETIME, String.valueOf(poolScheduleThreadLifeTime));
            return this;
        }

        public JSQLDataSource build() {
            return new JSQLDataSource(jdbcProperties);
        }
    }
}
