package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-12-12
 */
public abstract class AbstractDialect implements Dialect {
    public String getDialectName() {
        return null;
    }

    public void injectOffsetLimit(BuilderContext builderCtx) {
        throw new UnsupportedOperationException();
    }

    public boolean supportOffsetLimit() {
        return false;
    }

    public String wrapOffsetLimit(BuilderContext builderContext, String sql) {
        return sql;
    }
    public String wrapLimit(BuilderContext builderContext, String sql) {
        return sql;
    }
}
