package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.DeleteBuilder;
import cn.icuter.jsql.builder.InsertBuilder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.builder.UpdateBuilder;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.dialect.UnknownDialect;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.pool.DefaultObjectPool;
import cn.icuter.jsql.pool.ObjectPool;
import cn.icuter.jsql.pool.PooledObjectManager;

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
    private Dialect dialect; // parse by url or name or dialect class name

    /**
     * <pre>
     * jdbc.properties
     * - url             mandatory
     * - username        mandatory
     * - password        mandatory
     * - driverClass     optional if dialect set
     * - dialect         optional if driverClass set
     * </pre>
     * @param jdbcProp jdbc properties in file or create by manually
     */
    public JSQLDataSource(Properties jdbcProp) {
        String dialectInProp = jdbcProp.getProperty("dialect");
        Dialect dialect;
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
        init(jdbcProp.getProperty("url"), jdbcProp.getProperty("username"), jdbcProp.getProperty("password"),
                jdbcProp.getProperty("driverClass"), dialect);
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
        init(url, username, password, driverClassName, dialect);
    }

    private void init(String url, String username, String password, String driverClassName, Dialect dialect) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.dialect = dialect;
        if ((this.driverClassName == null || this.driverClassName.length() <= 0) && this.dialect != null) {
            this.driverClassName = dialect.getDriverClassName();
        }
    }

    public Connection newConnection() throws ClassNotFoundException, SQLException {
        Objects.requireNonNull(driverClassName, "Driver Class Name must not be null!");
        Class.forName(driverClassName);
        return DriverManager.getConnection(url, username, password);
    }

    public ObjectPool<Connection> createConnectionPool() {
        return createConnectionPool(null);
    }

    public ObjectPool<Connection> createConnectionPool(PoolConfiguration poolConfiguration) {
        PooledObjectManager<Connection> manager = new PooledConnectionManager(url, username, password,
                driverClassName);
        return poolConfiguration == null ? new DefaultObjectPool<>(manager)
                : new DefaultObjectPool<>(manager, poolConfiguration);
    }

    public ObjectPool<JdbcExecutor> createExecutorPool() {
        return createExecutorPool(null);
    }
    
    public ObjectPool<JdbcExecutor> createExecutorPool(PoolConfiguration poolConfiguration) {
        PooledObjectManager<JdbcExecutor> manager = new PooledExecutorManager(url, username, password, driverClassName);
        return poolConfiguration == null ? new DefaultObjectPool<>(manager)
                : new DefaultObjectPool<>(manager, poolConfiguration);
    }

    public SelectBuilder newSelectBuilder() {
        return new SelectBuilder(dialect);
    }
    public UpdateBuilder newUpdateBuilder() {
        return new UpdateBuilder(dialect);
    }
    public InsertBuilder newInsertBuilder() {
        return new InsertBuilder(dialect);
    }
    public DeleteBuilder newDeleteBuilder() {
        return new DeleteBuilder(dialect);
    }
}
