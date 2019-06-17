package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.builder.SQLStringBuilder;

/**
 * @author edward
 * @since 2018-08-30
 */
public class OracleDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }

    @Override
    public String getDialectName() {
        return "oracle";
    }

    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        int offset = builderCtx.getOffset();
        int limit = builderCtx.getLimit();
        String rowNumberAlias = Dialects.getRowNumberAlias(builderCtx);
        SQLStringBuilder sqlStringBuilder = builderCtx.getSqlStringBuilder();
        if (offset > 0) {
            sqlStringBuilder.prepend("select * from (select source_.*, rownum " + rowNumberAlias + " from (")
                    .append(") source_ where rownum <= ?) where " + rowNumberAlias + " > ?");
            builderCtx.getBuilder().value(limit + offset);
            builderCtx.getBuilder().value(offset);
        } else {
            sqlStringBuilder.prepend("select * from (").append(") where rownum <= ?");
            builderCtx.getBuilder().value(limit);
        }
    }

    @Override
    public String wrapOffsetLimit(BuilderContext builderContext, String sql) {
        String rowNumberAlias = Dialects.getRowNumberAlias(builderContext);
        return "select oracle_alias_.*, rownum as " + rowNumberAlias + " from (" + sql + ") oracle_alias_";
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }

    @Override
    public String getQuoteString() {
        return "\"";
    }
}
