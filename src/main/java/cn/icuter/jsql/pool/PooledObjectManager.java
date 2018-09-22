package cn.icuter.jsql.pool;

import cn.icuter.jsql.exception.JSQLException;

/**
 * @author edward
 * @since 2018-08-18
 */
public interface PooledObjectManager<T> {
    PooledObject<T> create() throws JSQLException;
    void invalid(PooledObject<T> pooledObject) throws JSQLException;
    boolean validate(PooledObject<T> pooledObject) throws JSQLException;
}
