package cn.icuter.jsql.datasource;

import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.pool.PooledObject;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author edward
 * @since 2018-12-23
 */
public class PooledConnection implements Connection {

    PooledObject<Connection> pooledObject;
    Connection connection;
    PooledConnection(Connection connection) {
        this.connection = connection;
        this.pooledObject = new PooledObject<Connection>(this);
    }
    @Override
    public Statement createStatement() throws SQLException {
        checkIsBorrowed();
        return connection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkIsBorrowed();
        return connection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        checkIsBorrowed();
        return connection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        checkIsBorrowed();
        return connection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkIsBorrowed();
        connection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkIsBorrowed();
        return connection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        checkIsBorrowed();
        connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        checkIsBorrowed();
        connection.rollback();
    }

    @Override
    public void close() throws SQLException {
        checkIsBorrowed();
        try {
            pooledObject.getObjectPool().returnObject(this);
        } catch (JSQLException e) {
            throw new SQLException("", e);
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkIsBorrowed();
        return connection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkIsBorrowed();
        connection.setReadOnly(true);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        checkIsBorrowed();
        return connection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        checkIsBorrowed();
        connection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        checkIsBorrowed();
        return connection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        checkIsBorrowed();
        connection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        checkIsBorrowed();
        return connection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkIsBorrowed();
        return connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkIsBorrowed();
        connection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        checkIsBorrowed();
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkIsBorrowed();
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkIsBorrowed();
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        checkIsBorrowed();
        return connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        checkIsBorrowed();
        connection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        checkIsBorrowed();
        connection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        checkIsBorrowed();
        return connection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        checkIsBorrowed();
        return connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        checkIsBorrowed();
        return connection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        checkIsBorrowed();
        connection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        checkIsBorrowed();
        connection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkIsBorrowed();
        return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkIsBorrowed();
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkIsBorrowed();
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        checkIsBorrowed();
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        checkIsBorrowed();
        return connection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        checkIsBorrowed();
        return connection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        checkIsBorrowed();
        return connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        checkIsBorrowed();
        return connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        checkIsBorrowed();
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        checkIsBorrowed();
        return connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        try {
            checkIsBorrowed();
        } catch (SQLException e) {
            throw new SQLClientInfoException(null, e);
        }
        connection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        try {
            checkIsBorrowed();
        } catch (SQLException e) {
            throw new SQLClientInfoException(null, e);
        }
        connection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        checkIsBorrowed();
        return connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        checkIsBorrowed();
        return connection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        checkIsBorrowed();
        return connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        checkIsBorrowed();
        return connection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        checkIsBorrowed();
        connection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        checkIsBorrowed();
        return connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        checkIsBorrowed();
        connection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        checkIsBorrowed();
        connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        checkIsBorrowed();
        return connection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        checkIsBorrowed();
        return connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        checkIsBorrowed();
        return connection.isWrapperFor(iface);
    }

    private void checkIsBorrowed() throws SQLException {
        if (!pooledObject.isBorrowed()) {
            throw new SQLException("PooledConnection has been return to pool");
        }
    }
}
