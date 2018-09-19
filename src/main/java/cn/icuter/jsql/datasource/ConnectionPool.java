package cn.icuter.jsql.datasource;

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
        } catch (Exception e) {
            // TODO log
            return null;
        }
    }

    public void returnConnection(Connection conn) {
        try {
            pool.returnObject(conn);
        } catch (Exception e) {
            // TODO log
        }
    }

    public void close() {
        try {
            pool.close();
        } catch (Exception e) {
            // TODO log
        }
    }
}
