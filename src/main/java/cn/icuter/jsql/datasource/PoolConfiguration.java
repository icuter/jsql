package cn.icuter.jsql.datasource;

import java.util.concurrent.TimeUnit;

/**
 * @author edward
 * @since 2018-08-11
 */
public class PoolConfiguration {

    /** Setting of max objects size in pool, and default is 20 */
    private int maxPoolSize;

    /**
     * <pre>
     * Pooled object idle timeout in milliseconds, default is 30 minutes
     *   -1 means never timeout
     *   0 means always timeout
     * </pre>
     */
    private long idleTimeout;

    /**
     * Validate on borrowing an object from pool
     * <br><br/>
     * <em>default: true</em>
     */
    private boolean validateOnBorrow;

    /**
     * Validate on returning an object to pool
     * <br><br/>
     * <em>default: false</em>
     */
    private boolean validateOnReturn;

    /**
     * <pre>
     * Poll object waiting in milliseconds
     *     0 : no wait, maybe return null
     *   0 < : wait permanently, will never return null
     * </pre>
     * <em>default 10 seconds</em>
     */
    private long pollTimeout;

    /** retry to create pool object if exception occur, default 0 */
    private int createRetryCount;

    /**
     * Life time of scheduled thread in milliseconds, but set it to negative or 0 means never timeout
     * <br>
     * <em>default 5 minutes</em>
     * @since v1.0.4
     */
    private long scheduledThreadLifeTime;

    public static PoolConfiguration defaultPoolCfg() {
        PoolConfiguration poolConfiguration = new PoolConfiguration();
        poolConfiguration.setMaxPoolSize(20);
        poolConfiguration.setPollTimeout(10000);
        poolConfiguration.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        poolConfiguration.setScheduledThreadLifeTime(TimeUnit.MINUTES.toMillis(5));
        poolConfiguration.setValidateOnBorrow(true);
        poolConfiguration.setValidateOnReturn(false);
        return poolConfiguration;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getCreateRetryCount() {
        return createRetryCount;
    }

    public void setCreateRetryCount(int createRetryCount) {
        this.createRetryCount = createRetryCount;
    }

    public void setValidateOnBorrow(boolean validateOnBorrow) {
        this.validateOnBorrow = validateOnBorrow;
    }

    public void setValidateOnReturn(boolean validateOnReturn) {
        this.validateOnReturn = validateOnReturn;
    }

    public boolean isValidateOnBorrow() {
        return validateOnBorrow;
    }

    public boolean isValidateOnReturn() {
        return validateOnReturn;
    }

    public long getScheduledThreadLifeTime() {
        return scheduledThreadLifeTime;
    }

    public void setScheduledThreadLifeTime(long scheduledThreadLifeTime) {
        this.scheduledThreadLifeTime = scheduledThreadLifeTime;
    }

    @Override
    public String toString() {
        return "PoolConfiguration{"
                + "maxPoolSize=" + maxPoolSize
                + ", idleTimeout=" + idleTimeout + "ms"
                + ", pollTimeout=" + pollTimeout + "ms"
                + ", scheduledThreadLifeTime=" + scheduledThreadLifeTime + "ms"
                + ", validateOnBorrow=" + validateOnBorrow
                + ", validateOnReturn=" + validateOnReturn
                + ", createRetryCount=" + createRetryCount
                + "}";
    }
}
