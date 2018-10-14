package cn.icuter.jsql.log;

/**
 * @author edward
 * @since 2018-10-05
 */
public interface JSQLLogger {
    void init(Class<?> clazz);
    void trace(String log);
    void trace(String log, Throwable e);
    void debug(String log);
    void debug(String log, Throwable e);
    void info(String log);
    void info(String log, Throwable e);
    void warn(String log);
    void warn(String log, Throwable e);
    void error(String log);
    void error(String log, Throwable e);
}
