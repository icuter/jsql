package cn.icuter.jsql.pool;

/**
 * @author edward
 * @since 2018-08-18
 */
public class PooledObject<T> {

    private final long createTime;
    private long lastBorrowedTime;
    private long lastReturnedTime;
    private boolean borrowed;
    private final T object;
    private ObjectPool<T> objectPool;

    public PooledObject(T object) {
        this.object = object;
        this.createTime = System.currentTimeMillis();
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getLastBorrowedTime() {
        return lastBorrowedTime;
    }

    public long getLastReturnedTime() {
        return lastReturnedTime;
    }

    public boolean isBorrowed() {
        return borrowed;
    }

    public T getObject() {
        return object;
    }

    void setObjectPool(ObjectPool<T> objectPool) {
        this.objectPool = objectPool;
    }

    public ObjectPool<T> getObjectPool() {
        return objectPool;
    }

    void updateLastBorrowedTime() {
        this.lastBorrowedTime = System.currentTimeMillis();
    }

    void updateLastReturnedTime() {
        this.lastReturnedTime = System.currentTimeMillis();
    }

    void markBorrowed() {
        borrowed = true;
    }

    void markReturned() {
        borrowed = false;
    }

    @Override
    public String toString() {
        return new StringBuilder("PooledObject{")
                .append("createTime=").append(createTime)
                .append(", lastBorrowedTime=").append(lastBorrowedTime)
                .append(", lastReturnedTime=").append(lastReturnedTime)
                .append(", borrowed=").append(borrowed)
                .append(", objectType=").append(object == null ? null : object.getClass().getName())
                .append('}').toString();
    }
}
