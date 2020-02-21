package cn.icuter.jsql.pool;

import java.util.concurrent.RunnableScheduledFuture;

/**
 * @author edward
 * @since 2018-08-18
 */
public class PooledObject<T> {

    private final long createTime;
    private volatile long lastBorrowedTime;
    private volatile long lastReturnedTime;
    private volatile boolean borrowed;
    private volatile boolean valid = true;
    private final T object;
    private ObjectPool<T> objectPool;
    RunnableScheduledFuture<?> scheduledFuture; // maintains idle task object

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

    public boolean isValid() {
        return valid;
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

    void setBorrowed() {
        borrowed = true;
    }

    void setReturned() {
        borrowed = false;
    }

    void setInvalid() {
        valid = false;
    }

    @Override
    public String toString() {
        return "PooledObject {"
                + "createTime=" + createTime
                + ", lastBorrowedTime=" + lastBorrowedTime
                + ", lastReturnedTime=" + lastReturnedTime
                + ", borrowed=" + borrowed
                + ", valid=" + valid
                + ", objectType=" + (object == null ? null : object.getClass().getName())
                + '}';
    }
}
