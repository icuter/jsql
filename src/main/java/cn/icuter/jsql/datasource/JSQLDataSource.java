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
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author edward
 * @since 2018-08-10
 */
public class JSQLDataSource extends AbstractBuilderDataSource implements javax.sql.ConnectionPoolDataSource, Closeable {

    private static final JSQLLogger LOGGER = Logs.getLogger(JSQLDataSource.class);

    private static final String PROP_PREFIX = "driver.";
    private static final String PROP_USER = "user";
    private static final String PROP_PASSWORD = "password";

    private String url;
    private String driverClassName;
    private Dialect dialect;        // parse by url or name or dialect class name
    private int loginTimeout;       // default 5s
    private Properties driverProps = new Properties(); // driver get connection properties
    private ConnectionPool connectionPool;
    private JdbcExecutorPool executorPool;

    protected JSQLDataSource() {
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
        String dialectInProp = jdbcProp.getProperty("dialect");
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
            dialect = Dialects.parseUrl(jdbcProp.getProperty("url"));
        }
        int loginTimeout = Integer.parseInt(jdbcProp.getProperty("loginTimeout", "5"));
        setDriverProps(jdbcProp);
        init(jdbcProp.getProperty("url"), jdbcProp.getProperty("username"), jdbcProp.getProperty("password"),
                jdbcProp.getProperty("driverClass"), loginTimeout, dialect);
        initPool(jdbcProp);
    }

    private void setDriverProps(Properties jdbcProp) {
        Set<Object> keySet = jdbcProp.keySet();
        for (Object keyObj : keySet) {
            String key = (String) keyObj;
            if (key.startsWith(PROP_PREFIX)) {
                driverProps.put(key.substring(PROP_PREFIX.length()), jdbcProp.getProperty(key));
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
        if (dialect != null && dialect.requireUserPassword()
                && !driverProps.containsKey(PROP_USER)
                && !driverProps.containsKey(PROP_PASSWORD)) {
            ObjectUtil.requireNonEmpty(username, "username must not be empty");
            ObjectUtil.requireNonNull(password, "password must not be null");
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
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        if (poolProp.containsKey("pool.maxPoolSize")) {
            poolConfiguration.setMaxPoolSize(Integer.parseInt(poolProp.getProperty("pool.maxPoolSize")));
        }
        if (poolProp.containsKey("pool.idleTimeout")) {
            poolConfiguration.setIdleTimeout(Long.parseLong(poolProp.getProperty("pool.idleTimeout")));
        }
        if (poolProp.containsKey("pool.validateOnBorrow")) {
            poolConfiguration.setValidateOnBorrow(Boolean.parseBoolean(poolProp.getProperty("pool.validateOnBorrow")));
        }
        if (poolProp.containsKey("pool.validateOnReturn")) {
            poolConfiguration.setValidateOnReturn(Boolean.parseBoolean(poolProp.getProperty("pool.validateOnReturn")));
        }
        if (poolProp.containsKey("pool.pollTimeout")) {
            poolConfiguration.setPollTimeout(Long.parseLong(poolProp.getProperty("pool.pollTimeout")));
        }
        if (poolProp.containsKey("pool.createRetryCount")) {
            poolConfiguration.setCreateRetryCount(Integer.parseInt(poolProp.getProperty("pool.createRetryCount")));
        }
        if (poolProp.containsKey("pool.scheduledThreadLifeTime")) {
            poolConfiguration.setScheduledThreadLifeTime(Long.parseLong(poolProp.getProperty("pool.scheduledThreadLifeTime")));
        }
        ObjectPool<Connection> objectPool = createConnectionObjectPool(poolConfiguration);
        connectionPool = new ConnectionPool(objectPool);
        executorPool = new JdbcExecutorPool(objectPool);
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
            LOGGER.info("error occurs while doing transaction operation and has been rolled back");
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

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
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
        return new StringBuilder("JSQLDataSource{")
                .append("url='").append(url).append('\'')
                .append(", driverProps='").append(displayDriverProps()).append('\'')
                .append(", driverClassName='").append(driverClassName).append('\'')
                .append(", dialect='").append(dialect.getDialectName()).append('\'')
                .append(", loginTimeout=").append(loginTimeout).append("s")
                .append('}').toString();
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
}
