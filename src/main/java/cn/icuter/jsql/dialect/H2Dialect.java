package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-30
 */
public class H2Dialect extends AbstractDialect {
    @Override
    public String getDriverClassName() {
        return "org.h2.Driver";
    }

    @Override
    public String getDialectName() {
        return "h2";
    }

    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        Dialects.injectWithLimitOffsetKey(builderCtx);
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
