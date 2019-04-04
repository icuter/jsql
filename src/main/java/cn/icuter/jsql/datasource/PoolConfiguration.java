package cn.icuter.jsql.datasource;

import java.util.concurrent.TimeUnit;

/**
 * @author edward
 * @since 2018-08-11
 */
public class PoolConfiguration {

    /**
     * Setting of max objects size in pool, and default is 20
     */
    private int maxPoolSize;

    /**
     * <pre>
     * Setting of idle timeout with milliseconds, default is 30 minutes
     *   -1 means never timeout
     *   0 means always timeout
     * </pre>
     */
    private long idleTimeout;

    /**
     * validate on borrowing an object from pool
     * <br><br/>
     * <em>default: true</em>
     */
    private boolean validateOnBorrow;

    /**
     * validate on returning an object to pool
     * <br><br/>
     * <em>default: false</em>
     */
    private boolean validateOnReturn;

    private long pollTimeout;       // milliseconds, default 5 seconds

    /**
     * retry to create pool object if exception occur, default 0
     */
    private int createRetryCount;

    public static PoolConfiguration defaultPoolCfg() {
        PoolConfiguration poolConfiguration = new PoolConfiguration();
        poolConfiguration.setMaxPoolSize(20);
        poolConfiguration.setPollTimeout(5000);
        poolConfiguration.setIdleTimeout(TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES));
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

    @Override
    public String toString() {
        return "PoolConfiguration{"
                + "maxPoolSize=" + maxPoolSize
                + ", idleTimeout=" + idleTimeout + "ms"
                + ", pollTimeout=" + pollTimeout + "ms"
                + ", validateOnBorrow=" + validateOnBorrow
                + ", validateOnReturn=" + validateOnReturn
                + "}";
    }
}
