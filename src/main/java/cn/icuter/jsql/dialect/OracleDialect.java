package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.builder.SQLStringBuilder;
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
        SQLStringBuilder sqlStringBuilder = builderCtx.getSqlStringBuilder();
        if (offset > 0) {
            sqlStringBuilder.prepend("select * from (select _source.*, rownum _rownum from (");
            int forUpdateIndex = builderCtx.getForUpdatePosition(); // can't pre get forUpdate position
            if (forUpdateIndex > 0) {
                sqlStringBuilder.insert(forUpdateIndex, ") _source where rownum <= ?) where _rownum > ?");
            } else {
                sqlStringBuilder.append(") _source where rownum <= ?) where _rownum > ?");
            }
            builderCtx.addCondition(Cond.value(limit));
            builderCtx.addCondition(Cond.value(offset));
        } else {
            sqlStringBuilder.prepend("select * from (");
            int forUpdateIndex = builderCtx.getForUpdatePosition(); // can't pre get forUpdate position
            if (forUpdateIndex > 0) {
                sqlStringBuilder.insert(forUpdateIndex, ") where rownum <= ?");
            } else {
                sqlStringBuilder.append(") where rownum <= ?");
            }
            builderCtx.addCondition(Cond.value(limit));
        }
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
