package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.builder.SQLStringBuilder;
import cn.icuter.jsql.condition.Cond;

/**
 * @author edward
 * @since 2018-08-30
 */
public class SQLServer2012PlusDialect extends AbstractDialect {
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
            throw new IllegalArgumentException("must define [order by] clause!");
        }
        SQLStringBuilder sqlStringBuilder = builderCtx.getSqlStringBuilder();

        sqlStringBuilder.append("offset");
        if (builderCtx.getOffset() > 0) {
            sqlStringBuilder.append("?");
            builderCtx.addCondition(Cond.value(builderCtx.getOffset()));
        } else {
            sqlStringBuilder.append("0");
        }
        sqlStringBuilder.append("rows fetch next ? rows only");
        builderCtx.addCondition(Cond.value(builderCtx.getLimit()));
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
