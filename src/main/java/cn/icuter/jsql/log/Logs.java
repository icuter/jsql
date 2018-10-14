package cn.icuter.jsql.log;

/**
 * @author edward
 * @since 2018-10-05
 */
public abstract class Logs {
    private static Class<? extends JSQLLogger> loggerClass;
    static {
        // 1. check slf4j
        // 2. check log4j2.x
        // 3. check log4j1.x
        // 4. check JUL(java.util.logging.Logger)
        // 5. don't check logback, because, generally, logback will be used as slf4j, no need to check logback Logger
        if (SLF4JLogger.exists()) {
            loggerClass = SLF4JLogger.class;
        } else if (Log4j2Logger.exists()) {
            loggerClass = Log4j2Logger.class;
        } else if (Log4jLogger.exists()) {
            loggerClass = Log4jLogger.class;
        } else {
            loggerClass = JDKLogger.class;
        }
    }

    public static JSQLLogger getLogger(Class<?> clazz) {
        try {
            JSQLLogger jsqlLogger = loggerClass.newInstance();
            jsqlLogger.init(clazz);
            return jsqlLogger;
        } catch (InstantiationException | IllegalAccessException e) {
            JDKLogger jdkLogger = new JDKLogger();
            jdkLogger.init(clazz);
            jdkLogger.warn("no logger for " + loggerClass.getName() + ", using JDKLogger instead", e);
            return jdkLogger;
        }
    }

    public static void setLogger(Class<? extends JSQLLogger> loggerClass) {
        Logs.loggerClass = loggerClass;
    }
}
