package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-30
 */
public class CubridDialect extends AbstractDialect {
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

    @Override
    public boolean supportConnectionIsValid() {
        return false;
    }

    @Override
    public String validationSql() {
        return "select 1";
    }

    @Override
    public boolean supportSavepoint() {
        return false;
    }

    @Override
    public boolean supportNClob() {
        return false;
    }
}
