package cn.icuter.jsql.pool;

/**
 * @author edward
 * @since 2018-08-18
 */
public interface PooledObjectManager<T> {
    PooledObject<T> create() throws Exception;
    void invalid(PooledObject<T> pooledObject) throws Exception;
    boolean validate(PooledObject<T> pooledObject) throws Exception;
}
