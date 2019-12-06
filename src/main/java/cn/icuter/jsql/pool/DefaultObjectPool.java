package cn.icuter.jsql.pool;

import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolException;
import cn.icuter.jsql.exception.PooledObjectCreationException;
import cn.icuter.jsql.exception.PooledObjectPollTimeoutException;
import cn.icuter.jsql.exception.PooledObjectReturnException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author edward
 * @since 2018-08-11
 */
public class DefaultObjectPool<T> implements ObjectPool<T> {

    private static final JSQLLogger LOGGER = Logs.getLogger(DefaultObjectPool.class);

    private static final int IDLE_NEVER_TIMEOUT = -1;
    private static final int IDLE_ALWAYS_TIMEOUT = 0;
    private static final int IDLE_SCHEDULE_OFFSET_MILLISECONDS = 100; // check idle timeout delay 100ms

    private PoolConfiguration poolCfg;
    private final PooledObjectManager<T> manager;
    private BlockingDeque<PooledObject<T>> idlePooledObjects = new LinkedBlockingDeque<>();
    private Map<Integer, PooledObject<T>> allPooledObjects;

    private ReentrantLock createLock = new ReentrantLock();
    private ReadWriteLock poolLock = new ReentrantReadWriteLock();
    private Lock closeLock = poolLock.writeLock();
    private Lock opLock = poolLock.readLock();

    private volatile boolean closed;
    private PoolStats poolStats = new PoolStats();
    private IdleObjectScheduledExecutor idleObjectExecutor;

    public DefaultObjectPool(PooledObjectManager<T> manager) {
        this(manager, PoolConfiguration.defaultPoolCfg());
    }

    public DefaultObjectPool(PooledObjectManager<T> manager,
                             PoolConfiguration poolConfiguration) {
        this.manager = manager;
        initPool(poolConfiguration);

        LOGGER.debug("set up object pool with pool configuration: " + this.poolCfg);
    }

    private void initPool(PoolConfiguration poolConfiguration) {
        this.poolCfg = poolConfiguration;
        if (poolCfg.getMaxPoolSize() <= 0) {
            throw new IllegalArgumentException("max pool size must not be zero!");
        }
        this.allPooledObjects = new ConcurrentHashMap<>(this.poolCfg.getMaxPoolSize());

        long idleObjectTimeout = poolCfg.getIdleTimeout();
        if (idleObjectTimeout > 0) {
            idleObjectExecutor = new IdleObjectScheduledExecutor(1);
            if (poolCfg.getScheduledThreadLifeTime() > 0) {
                idleObjectExecutor.setKeepAliveTime(poolCfg.getScheduledThreadLifeTime(), TimeUnit.MILLISECONDS);
                idleObjectExecutor.allowCoreThreadTimeOut(true);
            }
        }
    }

    @Override
    public T borrowObject() throws JSQLException {
        PooledObject<T> pc = getPooledObject();
        if (pc == null) {
            return null;
        }
        pc.markBorrowed();
        pc.updateLastBorrowedTime();
        poolStats.updateBorrowStats();
        poolStats.updateLastAccessTime();
        return pc.getObject();
    }

    private PooledObject<T> getPooledObject() throws JSQLException {
        PooledObject<T> pooledObject;
        do {
            opLock.lock();
            try {
                if (isPoolClosed()) {
                    throw new PoolException("get pooled object fail, due to pool was already closed!");
                }
                pooledObject = idlePooledObjects.pollFirst();
                // queue is empty and pool not full, try to create one
                if (pooledObject == null && isPoolNotFull()) {
                    createLock.lock();
                    try {
                        if (isPoolNotFull()) {
                            pooledObject = tryToCreate(0);
                            if (pooledObject == null) {
                                // should never happen
                                throw new PooledObjectCreationException("create pool object error!");
                            }
                            pooledObject.setObjectPool(this);
                            allPooledObjects.put(System.identityHashCode(pooledObject.getObject()), pooledObject);
                            poolStats.updateCreateStats();

                            LOGGER.trace("pooled object has been created, object detail: " + pooledObject);
                            break;
                        }
                    } finally {
                        createLock.unlock();
                    }
                }
                // pool reach the max size
                if (pooledObject == null) {
                    if (poolCfg.getPollTimeout() > 0) {
                        try {
                            pooledObject = idlePooledObjects.pollFirst(poolCfg.getPollTimeout(), TimeUnit.MILLISECONDS);
                            if (pooledObject == null) {
                                throw new PooledObjectPollTimeoutException("get pool object timeout, waited for "
                                        + poolCfg.getPollTimeout() + "ms");
                            }
                        } catch (InterruptedException e) {
                            throw new PoolException("get pool object fail!", e);
                        }
                    } else {
                        try {
                            // setting poll timeout args for releasing CPU resource
                            pooledObject = idlePooledObjects.pollFirst(100L, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
                if (pooledObject != null) {
                    if (validateFailOnBorrow(pooledObject)) {
                        invalidPooledObject(pooledObject);
                        continue;
                    } else {
                        break;
                    }
                }
                if (isPollNoWait()) {
                    return null;
                }
                LOGGER.trace("get pooled object failed, continue to get the next");
            } finally {
                opLock.unlock();
            }
        } while (true);
        return pooledObject;
    }

    private boolean isPollNoWait() {
        return poolCfg.getPollTimeout() == 0;
    }

    private boolean validateFailOnBorrow(PooledObject<T> pooledObject) throws JSQLException {
        return poolCfg.isValidateOnBorrow() && !manager.validate(pooledObject);
    }

    private PooledObject<T> tryToCreate(int tryCount) throws JSQLException {
        try {
            return manager.create();
        } catch (JSQLException e) {
            if (tryCount < poolCfg.getCreateRetryCount()) {
                LOGGER.warn("retry to create pool object with try count: " + (tryCount + 1));
                return tryToCreate(tryCount + 1);
            }
            if (tryCount > 0) {
                LOGGER.error("try to create pool object fail when exceeded retrying count: "
                        + poolCfg.getCreateRetryCount());
            }
            throw e;
        }
    }

    private boolean isPoolObjectIdleTimeout(PooledObject<T> pooledObject) {
        return isAlwaysIdleTimeout()
                || !isNeverIdleTimeout() && pooledObject.getLastReturnedTime() > 0
                && System.currentTimeMillis() - pooledObject.getLastReturnedTime() >= poolCfg.getIdleTimeout();
    }

    private boolean isAlwaysIdleTimeout() {
        return poolCfg.getIdleTimeout() == IDLE_ALWAYS_TIMEOUT;
    }

    private boolean isNeverIdleTimeout() {
        return poolCfg.getIdleTimeout() <= IDLE_NEVER_TIMEOUT;
    }

    private boolean isPoolNotFull() {
        return poolStats.getPoolSize() < poolCfg.getMaxPoolSize();
    }

    boolean isPoolEmpty() {
        return poolStats.getPoolSize() == 0;
    }

    private void invalidPooledObject(PooledObject<T> pooledObject) throws JSQLException {
        if (allPooledObjects.remove(System.identityHashCode(pooledObject.getObject())) != null) {
            manager.invalid(pooledObject);
            poolStats.updateRemoveStats();
        }
    }

    @Override
    public void returnObject(T object) throws JSQLException {
        if (object == null) {
            // ignore
            LOGGER.warn("returning object is null, no object will be returned");
            return;
        }
        PooledObject<T> pooledObject = getPooledObject(object);

        Objects.requireNonNull(pooledObject, "no such object in pool!");

        if (!pooledObject.isBorrowed()) {
            throw new PooledObjectReturnException("Object has been returned!");
        }
        poolStats.updateLastAccessTime();
        opLock.lock();
        try {
            if (isPoolClosed() || isAlwaysIdleTimeout() || validateFailOnReturn(pooledObject)) {
                invalidPooledObject(pooledObject);
                removeIdleScheduledTask(pooledObject);
                return;
            }
            pooledObject.markReturned();
            pooledObject.updateLastReturnedTime();
            removeIdleScheduledTask(pooledObject);
            scheduleIdleTimeoutTask(pooledObject);
            idlePooledObjects.addLast(pooledObject);
            poolStats.updateReturnStats();
        } finally {
            opLock.unlock();
        }
    }

    private void removeIdleScheduledTask(PooledObject<T> pooledObject) {
        if (idleObjectExecutor != null) {
            idleObjectExecutor.getQueue().remove(pooledObject.scheduledFuture);
        }
    }

    private boolean validateFailOnReturn(PooledObject<T> pooledObject) throws JSQLException {
        return poolCfg.isValidateOnReturn() && !manager.validate(pooledObject);
    }

    private void scheduleIdleTimeoutTask(PooledObject<T> pooledObject) {
        if (idleObjectExecutor != null) {
            idleObjectExecutor.scheduleIdleTask(new IdleObjectTimeoutTask(pooledObject),
                    poolCfg.getIdleTimeout() + IDLE_SCHEDULE_OFFSET_MILLISECONDS);
        }
    }

    private PooledObject<T> getPooledObject(T object) {
        return allPooledObjects.get(System.identityHashCode(object));
    }

    @Override
    public void close() throws JSQLException {
        if (isPoolClosed()) {
            LOGGER.warn("object pool has been closed, would not close again");
            return;
        }
        closeLock.lock();
        try {
            if (isPoolClosed()) {
                LOGGER.warn("object pool has been closed, would not close again");
                return;
            }
            closed = true;
            if (idleObjectExecutor != null) {
                idleObjectExecutor.shutdownNow();
            }
            PooledObject<T> pooledObject;
            while ((pooledObject = idlePooledObjects.poll()) != null) {
                invalidPooledObject(pooledObject);
            }
        } finally {
            closeLock.unlock();
        }
        LOGGER.debug("succeed in closing object pool, for more info: " + debugInfo());
    }

    @Override
    protected final void finalize() throws Throwable {
        try {
            close();
            // force to invalid all pooled object
            forceInvalidPooledObjects();
        } finally {
            super.finalize();
        }
    }

    private void forceInvalidPooledObjects() throws Exception {
        for (Map.Entry<Integer, PooledObject<T>> entry : allPooledObjects.entrySet()) {
            invalidPooledObject(entry.getValue());
        }
    }

    boolean isPoolClosed() {
        return closed;
    }

    @Override
    public String debugInfo() {
        return "pool state: " + (closed ? "CLOSED" : "RUNNING") + ", " + poolStats + ", " + poolCfg
                + ", idle schedule service info: "
                + (idleObjectExecutor != null ? idleObjectExecutor.toString() : "NOT RUNNING")
                + ", idle object size: " + idlePooledObjects.size();
    }

    PoolStats getPoolStats() {
        return poolStats;
    }

    class PoolStats {
        long poolSize;
        long createdCnt;
        long invalidCnt;
        long borrowedCnt;
        long returnedCnt;
        volatile long lastAccessTime;
        volatile String formattedLastAccessTime;

        PoolStats() {
            lastAccessTime = System.currentTimeMillis();
        }

        synchronized long getPoolSize() {
            return poolSize;
        }
        synchronized void updateCreateStats() {
            poolSize++;
            createdCnt++;
        }
        synchronized void updateRemoveStats() {
            poolSize--;
            invalidCnt++;
        }
        synchronized void updateBorrowStats() {
            borrowedCnt++;
        }
        synchronized void updateReturnStats() {
            returnedCnt++;
        }
        void updateLastAccessTime() {
            lastAccessTime = System.currentTimeMillis();
            LocalDateTime localDateTime = new Timestamp(lastAccessTime).toLocalDateTime();
            formattedLastAccessTime = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public String toString() {
            return "PoolStats {poolSize=" + poolSize
                    + ", createdCnt=" + createdCnt
                    + ", invalidCnt=" + invalidCnt
                    + ", borrowedCnt=" + borrowedCnt
                    + ", returnedCnt=" + returnedCnt
                    + ", lastAccessTime=" + formattedLastAccessTime + "}";
        }
    }

    class IdleObjectScheduledExecutor extends ScheduledThreadPoolExecutor {
        IdleObjectScheduledExecutor(int corePoolSize) {
            super(corePoolSize);
        }
        void scheduleIdleTask(IdleObjectTimeoutTask task, long delay) {
            task.pooledObject.scheduledFuture = (RunnableScheduledFuture) schedule(task, delay, TimeUnit.MILLISECONDS);
        }
    }
    private static final JSQLLogger TASK_LOGGER = Logs.getLogger(DefaultObjectPool.IdleObjectTimeoutTask.class);
    class IdleObjectTimeoutTask implements Runnable {
        private PooledObject<T> pooledObject;

        IdleObjectTimeoutTask(PooledObject<T> pooledObject) {
            this.pooledObject = pooledObject;
        }

        @Override
        public void run() {
            // actually, while object pool has been closed, that would never run its' scheduled task
            if (pooledObject != null && !pooledObject.isBorrowed() && !isPoolClosed() && isPoolObjectIdleTimeout(pooledObject)) {
                try {
                    if (idlePooledObjects.removeIf(p -> p == pooledObject)) {
                        invalidPooledObject(pooledObject);
                    }
                } catch (JSQLException e) {
                    TASK_LOGGER.error("invaliding pooled object error", e);
                }
            }
        }
    }
}
