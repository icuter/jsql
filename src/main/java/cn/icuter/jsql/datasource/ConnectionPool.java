package cn.icuter.jsql.datasource;

import cn.icuter.jsql.exception.BorrowObjectException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolCloseException;
import cn.icuter.jsql.exception.ReturnObjectException;
import cn.icuter.jsql.pool.ObjectPool;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import java.sql.Connection;

/**
 * @author edward
 * @since 2018-09-13
 */
public class ConnectionPool implements javax.sql.PooledConnection {

    private final ObjectPool<Connection> pool;

    ConnectionPool(ObjectPool<Connection> pool) {
        this.pool = pool;
    }

    @Override
    public Connection getConnection() {
        try {
            return pool.borrowObject();
        } catch (JSQLException e) {
            throw new BorrowObjectException("getting Connection error", e);
        }
    }

    public void returnConnection(Connection conn) {
        try {
            pool.returnObject(conn);
        } catch (JSQLException e) {
            throw new ReturnObjectException("returning Connection error", e);
        }
    }

    @Override
    public void close() {
        try {
            pool.close();
        } catch (JSQLException e) {
            throw new PoolCloseException("closing ConnectionPool error", e);
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {
    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {
    }
}
