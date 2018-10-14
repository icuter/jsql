package cn.icuter.jsql.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author edward
 * @since 2018-10-05
 */
public class SLF4JLogger implements JSQLLogger {
    protected Logger logger;

    static boolean exists() {
        try {
            Class.forName("org.slf4j.Logger");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void init(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
    }

    @Override
    public void trace(String log) {
        logger.trace(log);
    }

    @Override
    public void trace(String log, Throwable e) {
        logger.trace(log, e);
    }

    @Override
    public void debug(String log) {
        logger.debug(log);
    }

    @Override
    public void debug(String log, Throwable e) {
        logger.debug(log, e);
    }

    @Override
    public void info(String log) {
        logger.info(log);
    }

    @Override
    public void info(String log, Throwable e) {
        logger.info(log, e);
    }

    @Override
    public void warn(String log) {
        logger.warn(log);
    }

    @Override
    public void warn(String log, Throwable e) {
        logger.warn(log, e);
    }

    @Override
    public void error(String log) {
        logger.error(log);
    }

    @Override
    public void error(String log, Throwable e) {
        logger.error(log, e);
    }
}
