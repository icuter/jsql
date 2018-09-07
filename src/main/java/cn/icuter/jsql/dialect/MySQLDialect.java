package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-29
 */
public class MySQLDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getDialectName() {
        return "mysql"; // jdbc:mysql:
    }

    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        Dialects.injectWithLimitKey(builderCtx);
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
