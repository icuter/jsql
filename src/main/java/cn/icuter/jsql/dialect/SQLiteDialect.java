package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-09-10
 */
public class SQLiteDialect extends AbstractDialect {
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

    @Override
    public boolean supportBlob() {
        return false;
    }

    @Override
    public boolean supportClob() {
        return false;
    }

    @Override
    public boolean supportNClob() {
        return false;
    }

    @Override
    public boolean requireUserPassword() {
        return false;
    }
}
