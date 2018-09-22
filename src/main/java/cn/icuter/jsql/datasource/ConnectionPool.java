package cn.icuter.jsql.datasource;

import cn.icuter.jsql.exception.BorrowObjectException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolCloseException;
import cn.icuter.jsql.exception.ReturnObjectException;
import cn.icuter.jsql.pool.ObjectPool;

import java.sql.Connection;

/**
 * @author edward
 * @since 2018-09-13
 */
public class ConnectionPool {

    final ObjectPool<Connection> pool;

    ConnectionPool(ObjectPool<Connection> pool) {
        this.pool = pool;
    }

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

    public void close() {
        try {
            pool.close();
        } catch (JSQLException e) {
            throw new PoolCloseException("closing ConnectionPool error", e);
        }
    }
}
