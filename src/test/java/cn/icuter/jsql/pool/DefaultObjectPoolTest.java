package cn.icuter.jsql.pool;

import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PooledObjectPollTimeoutException;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author edward
 * @since 2018-08-28
 */
public class DefaultObjectPoolTest {

    private static PooledObjectManager<Object> manager;

    @BeforeClass
    public static void setup() {
        manager = new PooledObjectManager<Object>() {
            @Override
            public PooledObject<Object> create() throws JSQLException {
                return new PooledObject<Object>(new Object());
            }
            @Override
            public void invalid(PooledObject<Object> pooledObject) throws JSQLException {
                // noop
            }
            @Override
            public boolean validate(PooledObject<Object> pooledObject) throws JSQLException {
                return pooledObject.getObject() != null;
            }
        };
    }

    @Test
    public void testPoolStat() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        DefaultObjectPool.PoolStats poolStats = pool.getPoolStats();
        Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
        for (int i = 0; i < borrowedObjects.length; i++) {
            Object obj = pool.borrowObject();
            borrowedObjects[i] = obj;
            assertNotNull(obj);
        }
        assertEquals(poolStats.poolSize, cfg.getMaxPoolSize());
        assertEquals(poolStats.borrowedCnt, borrowedObjects.length);
        assertEquals(poolStats.returnedCnt, 0);
        assertEquals(poolStats.lastAccessTime, System.currentTimeMillis(), 1000L);
        assertEquals(poolStats.invalidCnt, 0);

        for (Object borrowedObject : borrowedObjects) {
            pool.returnObject(borrowedObject);
        }
        assertEquals(poolStats.returnedCnt, borrowedObjects.length);

        pool.close();

        assertEquals(poolStats.invalidCnt, cfg.getMaxPoolSize());
        assertTrue(pool.isPoolEmpty());
        assertTrue(pool.isPoolClosed());
    }

    @Test
    public void testClosePool() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
        for (int i = 0; i < borrowedObjects.length; i++) {
            Object obj = pool.borrowObject();
            borrowedObjects[i] = obj;
            assertNotNull(obj);
        }
        Thread.sleep(1000L);

        pool.close();

        for (Object borrowedObject : borrowedObjects) {
            pool.returnObject(borrowedObject);
        }
        assertTrue(pool.isPoolClosed());
        assertTrue(pool.isPoolEmpty());
    }

    @Test
    public void testTimeoutPooledObject() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleTimeout(50L);        // 0.5s idle timeout
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
            for (int i = 0; i < borrowedObjects.length; i++) {
                Object obj = pool.borrowObject();
                borrowedObjects[i] = obj;
            }
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            // let pool maintainer try to purge idle object
            Thread.sleep(cfg.getIdleTimeout() * borrowedObjects.length);
            // no idle objects remove, because all pooled objects has been borrowed
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            for (Object borrowedObject : borrowedObjects) {
                pool.returnObject(borrowedObject);
            }
            // let pool maintainer try to purge idle object again
            Thread.sleep((cfg.getIdleTimeout() + 100) * borrowedObjects.length);
            assertTrue(pool.isPoolEmpty());
        } finally {
            pool.close();
        }

        cfg.setIdleTimeout(-1); // idle object never timeout
        pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
            for (int i = 0; i < borrowedObjects.length; i++) {
                Object obj = pool.borrowObject();
                borrowedObjects[i] = obj;
            }
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            // let pool maintainer try to purge idle object
            Thread.sleep(50 * borrowedObjects.length);
            // no idle objects remove, because all pooled objects has been borrowed
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            for (Object borrowedObject : borrowedObjects) {
                pool.returnObject(borrowedObject);
            }
            // let pool maintainer try to purge idle object again
            Thread.sleep(50 * borrowedObjects.length);
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
        } finally {
            pool.close();
        }
    }

    @Test
    public void testMultiThread() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleTimeout(50L);        // 50ms idle timeout
        final DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            Thread[] threads = new Thread[80];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Object obj = null;
                        try {
                            obj = pool.borrowObject();
                            Thread.sleep(1000L);
                        } catch (Exception e) {
                            Assume.assumeNoException(e);
                        } finally {
                            if (obj != null) {
                                try {
                                    pool.returnObject(obj);
                                } catch (Exception e) {
                                    Assume.assumeNoException(e);
                                }
                            }
                        }
                    }
                });
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join();
            }
            assertTrue(pool.getPoolStats().poolSize <= cfg.getMaxPoolSize());

            // sleep enough time for schedule service run out
            Thread.sleep(1150L);

            assertTrue(pool.isPoolEmpty());
        } finally {
            pool.close();
        }
    }

    @Test
    public void testDefaultConfig() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            List<Object> pooledObjectList = new LinkedList<Object>();
            for (int i = 0; i < cfg.getMaxPoolSize(); i++) {
                pooledObjectList.add(pool.borrowObject());
            }
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().getPoolSize());
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().createdCnt);
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().borrowedCnt);
            for (Object obj : pooledObjectList) {
                pool.returnObject(obj);
            }
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().returnedCnt);
        } finally {
            pool.close();
        }
    }

    @Test
    public void testNoWait() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setPollTimeout(0);
        cfg.setMaxPoolSize(1);
        // no wait
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            Object obj = pool.borrowObject();
            assertNull(pool.borrowObject());
            pool.returnObject(obj);
        } finally {
            pool.close();
        }
    }

    @Test
    public void testWaitNeverTimeout() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setPollTimeout(-1);
        cfg.setMaxPoolSize(1);
        // never timeout
        final DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            Object firstBorrow = pool.borrowObject();
            new Thread() {
                @Override
                public void run() {
                    Object secondBorrow = null;
                    try {
                        secondBorrow = pool.borrowObject();
                        assertNotNull(secondBorrow);
                    } catch (JSQLException e) {
                        Assume.assumeNoException(e);
                    } finally {
                        try {
                            pool.returnObject(secondBorrow);
                        } catch (JSQLException e) {
                            Assume.assumeNoException(e);
                        }
                    }
                }
            }.start();
            Thread.sleep(6000L); // assume using the first borrowed object
            pool.returnObject(firstBorrow);
        } finally {
            pool.close();
        }
    }

    @Test
    public void testNoIdleTimeout() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleTimeout(0);
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            for (int i = 0; i < cfg.getMaxPoolSize(); i++) {
                Object object = pool.borrowObject();
                pool.returnObject(object);
                assertTrue(pool.isPoolEmpty());
            }
        } finally {
            pool.close();
        }
    }

    @Test(expected = PooledObjectPollTimeoutException.class)
    public void testBorrowObjectTimeout() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setPollTimeout(100L);
        cfg.setMaxPoolSize(1);
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            pool.borrowObject();
            pool.borrowObject();
        } finally {
            pool.close();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testReturnException() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        DefaultObjectPool<Object> pool = new DefaultObjectPool<Object>(manager, cfg);
        try {
            pool.returnObject(new Object());
        } finally {
            pool.close();
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
    }

}
