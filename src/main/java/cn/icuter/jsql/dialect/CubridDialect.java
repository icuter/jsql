package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-30
 */
public class CubridDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "cubrid.jdbc.driver.CUBRIDDriver";
    }

    @Override
    public String getDialectName() {
        return "cubrid";
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
