package cn.icuter.jsql;

import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.security.Injections;

public abstract class Settings {
    public static void setLogger(Class<? extends JSQLLogger> loggerClass) {
        Logs.setLogger(loggerClass);
    }

    public static void setBlacklistPattern(String[] pattern) {
        Injections.setBlacklistPattern(pattern);
    }
    public static void addBlacklistPattern(String... pattern) {
        Injections.addBlacklistPattern(pattern);
    }
}
