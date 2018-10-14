package cn.icuter.jsql.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author edward
 * @since 2018-10-05
 */
public class JDKLogger implements JSQLLogger {
    protected Logger logger;

    @Override
    public void init(Class<?> clazz) {
        logger = Logger.getLogger(clazz.getName());
    }

    @Override
    public void trace(String log) {
        logInternal(Level.FINEST, log, null);
    }

    @Override
    public void trace(String log, Throwable e) {
        logInternal(Level.FINEST, log, e);
    }

    @Override
    public void debug(String log) {
        logInternal(Level.FINE, log, null);
    }

    @Override
    public void debug(String log, Throwable e) {
        logInternal(Level.FINE, log, e);
    }

    @Override
    public void info(String log) {
        logInternal(Level.INFO, log, null);
    }

    @Override
    public void info(String log, Throwable e) {
        logInternal(Level.INFO, log, e);
    }

    @Override
    public void warn(String log) {
        logInternal(Level.WARNING, log, null);
    }

    @Override
    public void warn(String log, Throwable e) {
        logInternal(Level.WARNING, log, e);
    }

    @Override
    public void error(String log) {
        logInternal(Level.SEVERE, log, null);
    }

    @Override
    public void error(String log, Throwable e) {
        logInternal(Level.SEVERE, log, e);
    }

    protected void logInternal(Level level, String log, Throwable e) {
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            String stackTraceClassName = stackTraceElement.getClassName();
            if (stackTraceClassName.equalsIgnoreCase(logger.getName())) {
                logger.logp(level, stackTraceClassName, stackTraceElement.getMethodName(), log, e);
                return;
            }
            String packageName = JSQLLogger.class.getPackage().getName();
            if (!stackTraceClassName.startsWith(packageName)) {
                int lastPointIndex = packageName.lastIndexOf('.');
                if (lastPointIndex > 0) {
                    String parentPackageName = packageName.substring(0, lastPointIndex);
                    if (stackTraceClassName.startsWith(parentPackageName)) {
                        logger.logp(level, stackTraceClassName, stackTraceElement.getMethodName(), log, e);
                        return;
                    }
                }
            }
        }
        logger.log(level, log, e);
    }
}
