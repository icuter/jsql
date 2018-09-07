package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.condition.Cond;

/**
 * @author edward
 * @since 2018-08-30
 */
public class SQLServer2012PlusDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getDialectName() {
        return "sqlserver";
    }

    /**
     * [FOR UPDATE] clause is invalid in SQLServer select syntax
     *
     * @param builderCtx BuilderContext from builder
     */
    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        if (!builderCtx.isHasOrderBy()) {
            throw new IllegalArgumentException("Must define [order by] clause!");
        }
        StringBuilder prepareSqlBuilder = builderCtx.getPreparedSql();
        prepareSqlBuilder.append(" offset");
        if (builderCtx.getOffset() > 0) {
            prepareSqlBuilder.append(" ?");
            builderCtx.getConditionList().add(Cond.value(builderCtx.getOffset()));
        } else {
            prepareSqlBuilder.append(" 0");
        }
        prepareSqlBuilder.append(" rows fetch next ? rows only");
        builderCtx.getConditionList().add(Cond.value(builderCtx.getLimit()));
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
