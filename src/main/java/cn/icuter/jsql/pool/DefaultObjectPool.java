package cn.icuter.jsql.pool;

import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolException;
import cn.icuter.jsql.exception.PoolMaintainerException;
import cn.icuter.jsql.exception.PooledObjectCreationException;
import cn.icuter.jsql.exception.PooledObjectPollTimeoutException;
import cn.icuter.jsql.exception.PooledObjectReturnException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
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

    private PoolConfiguration poolConfiguration;
    private final PooledObjectManager<T> manager;
    private BlockingDeque<PooledObject<T>> idlePooledObjects = new LinkedBlockingDeque<>();
    private Map<Integer, PooledObject<T>> allPooledObjects;

    private ReentrantLock createLock = new ReentrantLock();
    private ReadWriteLock poolLock = new ReentrantReadWriteLock();
    private Lock closeLock = poolLock.writeLock();
    private Lock opLock = poolLock.readLock();

    private volatile boolean closed;
    private PoolStats poolStats = new PoolStats();
    private Timer pooledObjectMaintainer;

    public DefaultObjectPool(PooledObjectManager<T> manager) {
        this(manager, PoolConfiguration.defaultPoolCfg());
    }

    public DefaultObjectPool(PooledObjectManager<T> manager,
                             PoolConfiguration poolConfiguration) {
        this.manager = manager;
        initPool(poolConfiguration);
    }

    private void initPool(PoolConfiguration poolConfiguration) {
        this.poolConfiguration = poolConfiguration;
        if (this.poolConfiguration.getMaxPoolSize() <= 0) {
            throw new IllegalArgumentException("Max pool size must not be zero!");
        }
        this.allPooledObjects = new ConcurrentHashMap<>(this.poolConfiguration.getMaxPoolSize());

        long idleCheckInterval = this.poolConfiguration.getIdleCheckInterval();
        if (idleCheckInterval > 0) {
            pooledObjectMaintainer = new Timer(true);
            pooledObjectMaintainer.schedule(new PooledObjectMaintainerTask(), idleCheckInterval, idleCheckInterval);
        }
    }

    @Override
    public T borrowObject() throws JSQLException {
        PooledObject<T> pc = getPooledObject();
        pc.markBorrowed();
        pc.updateLastBorrowedTime();
        poolStats.updateLastAccessTime();
        return pc.getObject();
    }

    private PooledObject<T> getPooledObject() throws JSQLException {
        PooledObject<T> pooledObject;
        do {
            try {
                opLock.lock();
                if (isPoolClosed()) {
                    throw new PoolException("get pool object fail, due to pool was already closed!");
                }
                pooledObject = idlePooledObjects.pollFirst();
                // queue is empty and pool not full, try to create one
                if (pooledObject == null && isPoolNotFull()) {
                    try {
                        createLock.lock();
                        if (isPoolNotFull()) {
                            pooledObject = manager.create();
                            if (pooledObject == null) {
                                throw new PooledObjectCreationException("create pool object error"); // will never happen
                            }
                            pooledObject.setObjectPool(this);
                            allPooledObjects.put(System.identityHashCode(pooledObject.getObject()), pooledObject);
                            poolStats.updateCreateStats();
                            break;
                        }
                    } finally {
                        createLock.unlock();
                    }
                }
                // pool reach max size
                if (pooledObject == null) {
                    if (poolConfiguration.getPollTimeout() > 0) {
                        try {
                            pooledObject = idlePooledObjects.pollFirst(poolConfiguration.getPollTimeout(), TimeUnit.MILLISECONDS);
                            if (pooledObject == null) {
                                throw new PooledObjectPollTimeoutException("get pool object timeout, waited for "
                                        + poolConfiguration.getPollTimeout() + "ms");
                            }
                        } catch (InterruptedException e) {
                            throw new PoolException("get pool object fail!", e);
                        }
                    } else {
                        pooledObject = idlePooledObjects.pollFirst();
                    }
                }
                if (pooledObject != null) {
                    if (isIdleObjectTimeout(pooledObject) || !manager.validate(pooledObject)) {
                        manager.invalid(pooledObject);
                        removePooledObject(pooledObject);
                    } else {
                        break;
                    }
                }
            } finally {
                opLock.unlock();
            }
        } while (true);
        poolStats.updateBorrowStats();
        return pooledObject;
    }

    private boolean isIdleObjectTimeout(PooledObject<T> pooledObject) {
        long lastReturnedTime = pooledObject.getLastReturnedTime();
        return lastReturnedTime > 0 && System.currentTimeMillis() - lastReturnedTime > poolConfiguration.getIdleTimeout();
    }

    private boolean isPoolNotFull() {
        return poolStats.getPoolSize() < poolConfiguration.getMaxPoolSize();
    }

    boolean isPoolEmpty() {
        return poolStats.getPoolSize() == 0;
    }

    private void removePooledObject(PooledObject pooledObject) {
        allPooledObjects.remove(System.identityHashCode(pooledObject.getObject()));
        poolStats.updateRemoveStats();
    }

    @Override
    public void returnObject(T object) throws JSQLException {
        if (object == null) {
            // ignore
            return;
        }
        PooledObject<T> pooledObject = getPooledObject(object);

        Objects.requireNonNull(pooledObject, "No such object in pool!");

        if (!pooledObject.isBorrowed()) {
            throw new PooledObjectReturnException("Object has been return !");
        }
        pooledObject.markReturned();
        pooledObject.updateLastReturnedTime();
        poolStats.updateLastAccessTime();
        try {
            opLock.lock();
            if (isPoolClosed() || !manager.validate(pooledObject)) {
                manager.invalid(pooledObject);
                removePooledObject(pooledObject);
                return;
            }
            idlePooledObjects.addLast(pooledObject);
            poolStats.updateReturnStats();
        } finally {
            opLock.unlock();
        }
    }

    private PooledObject<T> getPooledObject(T object) {
        return allPooledObjects.get(System.identityHashCode(object));
    }

    @Override
    public void close() throws JSQLException {
        if (isPoolClosed()) {
            return;
        }
        try {
            closeLock.lock();
            if (isPoolClosed()) {
                return;
            }
            closed = true;
            pooledObjectMaintainer.cancel();
            PooledObject<T> pooledObject = idlePooledObjects.poll();
            while (pooledObject != null) {
                manager.invalid(pooledObject);
                removePooledObject(pooledObject);
                pooledObject = idlePooledObjects.poll();
            }
        } finally {
            closeLock.unlock();
        }
    }

    @Override
    public final void finalize() throws Throwable {
        close();
        // force to invalid all pooled object
        forceInvalidPooledObjects();
        super.finalize();
    }

    private void forceInvalidPooledObjects() throws Exception {
        Set<Integer> pooledObjKeys = allPooledObjects.keySet();
        for (Integer key : pooledObjKeys) {
            PooledObject<T> pooledObject = allPooledObjects.get(key);
            manager.invalid(pooledObject);
            removePooledObject(pooledObject);
        }
    }

    boolean isPoolClosed() {
        return closed;
    }

    @Override
    public String debugInfo() {
        return "Pool State: " + (closed ? "closed" : "running") + ", " + poolStats + "; " + poolConfiguration
                + "; idle size: " + idlePooledObjects.size();
    }

    PoolStats getPoolStats() {
        return poolStats;
    }

    class PoolStats {
        volatile long poolSize;
        volatile long createdCnt;
        volatile long invalidCnt;
        volatile long borrowedCnt;
        volatile long returnedCnt;
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
        synchronized void updateLastAccessTime() {
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

    class PooledObjectMaintainerTask extends TimerTask {
        @Override
        public void run() {
            try {
                opLock.lock();
                if (isPoolClosed()) {
                    // should not be happen here, just in case!!
                    cancel();
                    return;
                }
                if (isPoolEmpty() || idlePooledObjects.isEmpty()) {
                    return;
                }
                long idleTimeout = poolConfiguration.getIdleTimeout();
                if (idleTimeout == -1) {
                    // idle pooled object never timeout
                    return;
                }
                final long checkingTime = System.currentTimeMillis();
                idlePooledObjects.removeIf(pooledObj -> {
                    if (checkingTime - pooledObj.getLastReturnedTime() > idleTimeout) {
                        try {
                            manager.invalid(pooledObj);
                        } catch (JSQLException e) {
                            throw new PoolMaintainerException("invaliding pooled object error", e);
                        } finally {
                            removePooledObject(pooledObj);
                        }
                        return true;
                    }
                    return false;
                });
            } finally {
                opLock.unlock();
            }
        }
    }
}
