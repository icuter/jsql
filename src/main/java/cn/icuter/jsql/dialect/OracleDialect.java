package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.condition.Cond;

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
        StringBuilder preparedSql = builderCtx.getPreparedSql();
        String forUpdateSql = null;
        int forUpdateIndex = builderCtx.getForUpdatePosition();
        if (forUpdateIndex > 0) {
            forUpdateSql = preparedSql.substring(forUpdateIndex, preparedSql.length());
            preparedSql.delete(forUpdateIndex, preparedSql.length());
        }
        if (offset > 0) {
            preparedSql.insert(0, "select * from ( select _source.*, rownum _rownum from ( ");
            preparedSql.append(" ) _source where rownum <= ?) where _rownum > ?");
            builderCtx.getConditionList().add(Cond.value(limit));
            builderCtx.getConditionList().add(Cond.value(offset));
        } else {
            preparedSql.insert(0, "select * from ( ");
            preparedSql.append(" ) where rownum <= ?");
            builderCtx.getConditionList().add(Cond.value(limit));
        }
        if (forUpdateSql != null && forUpdateSql.length() > 0) {
            preparedSql.append(forUpdateSql);
        }
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
