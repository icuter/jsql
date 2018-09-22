package cn.icuter.jsql.datasource;

import java.util.concurrent.TimeUnit;

/**
 * @author edward
 * @since 2018-08-11
 */
public class PoolConfiguration {

    private int maxPoolSize;    // default 26
    /**
     * idle timeout -1 means never timeout (default)
     * idle timeout 0 means always timeout
     */
    private long idleTimeout;       // milliseconds, default -1
    private long idleCheckInterval; // milliseconds, default 15 minus
    private long pollTimeout;       // milliseconds, default -1

    public static PoolConfiguration defaultPoolCfg() {
        PoolConfiguration poolConfiguration = new PoolConfiguration();
        poolConfiguration.setMaxPoolSize(26);
        poolConfiguration.setPollTimeout(-1); // never timeout
        poolConfiguration.setIdleTimeout(-1); // never timeout
        poolConfiguration.setIdleCheckInterval(TimeUnit.MILLISECONDS.convert(15L, TimeUnit.MINUTES));
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
                + ", idleTimeout=" + idleTimeout
                + ", idleCheckInterval=" + idleCheckInterval
                + ", pollTimeout=" + pollTimeout + '}';
    }
}
