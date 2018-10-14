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
     * Setting of idle timeout with milliseconds, default is 1h
     *   -1 means never timeout
     *   0 means always timeout
     * </pre>
     */
    private long idleTimeout;       // milliseconds, default 1 hour

    /**
     * Setting idle object checking with pool object maintainer, default is half of {@link #idleTimeout}
     */
    private long idleCheckInterval; // milliseconds, default half of idleTimeout
    private long pollTimeout;       // milliseconds, default 5 seconds

    public static PoolConfiguration defaultPoolCfg() {
        PoolConfiguration poolConfiguration = new PoolConfiguration();
        poolConfiguration.setMaxPoolSize(20);
        poolConfiguration.setPollTimeout(5000);
        poolConfiguration.setIdleTimeout(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
        poolConfiguration.setIdleCheckInterval(poolConfiguration.getIdleTimeout() / 2);
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

    public long getIdleCheckInterval() {
        return idleCheckInterval;
    }

    public void setIdleCheckInterval(long idleCheckInterval) {
        this.idleCheckInterval = idleCheckInterval;
    }

    @Override
    public String toString() {
        return "PoolConfiguration{"
                + "maxPoolSize=" + maxPoolSize
                + ", idleTimeout=" + idleTimeout + "ms"
                + ", idleCheckInterval=" + idleCheckInterval + "ms"
                + ", pollTimeout=" + pollTimeout + "ms"
                + "}";
    }
}
