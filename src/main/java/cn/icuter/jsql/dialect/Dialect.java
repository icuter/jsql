package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-29
 */
public interface Dialect {
    String getDriverClassName();
    String getDialectName();

    void injectOffsetLimit(BuilderContext builderCtx);

    boolean supportOffsetLimit();

    String wrapOffsetLimit(BuilderContext builderContext, String sql);
    String wrapLimit(BuilderContext builderContext, String sql);
}
