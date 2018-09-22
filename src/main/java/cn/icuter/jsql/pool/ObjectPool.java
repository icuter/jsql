package cn.icuter.jsql.pool;

import cn.icuter.jsql.exception.JSQLException;

/**
 * @author edward
 * @since 2018-08-19
 */
public interface ObjectPool<T> extends AutoCloseable {

    /**
     * borrow the object from pool
     *
     * @return {@link cn.icuter.jsql.pool.PooledObject#getObject}
     * @throws Exception if pool was closed or borrow object time out
     */
    T borrowObject() throws JSQLException;

    /**
     * return the pooled object to pool, if pool was closed, the returning object will be invalided by {@link cn.icuter.jsql.pool.PooledObjectManager}
     *
     * @param object return object from {@link cn.icuter.jsql.pool.PooledObject#getObject}
     * @throws Exception while returning object occurs error
     */
    void returnObject(T object) throws JSQLException;

    /**
     * close the object pool, especially, while closing object pool, {@link #borrowObject}
     * and {@link #returnObject} will be lock till pool was closed.
     * <br/>
     * if pooled object was borrowed will not be invalided by {@link cn.icuter.jsql.pool.PooledObjectManager} but {@link #returnObject} do
     *
     * @throws Exception while closing object pool occurs error
     */
    void close() throws JSQLException;

    /**
     * show debug info
     *
     * @return formatted info
     */
    String debugInfo();
}
