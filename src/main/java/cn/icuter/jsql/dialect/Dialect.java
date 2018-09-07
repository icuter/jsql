package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-29
 */
public interface Dialect {
    String getDriverClassName();
    String getDialectName();

    default void injectOffsetLimit(BuilderContext builderCtx) {
        throw new UnsupportedOperationException();
    }

    default boolean supportOffsetLimit() {
        return false;
    }
}
