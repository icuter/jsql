package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-09-10
 */
public class SQLiteDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "org.sqlite.JDBC";
    }

    @Override
    public String getDialectName() {
        return "sqlite";
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
