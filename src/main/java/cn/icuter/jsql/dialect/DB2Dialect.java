package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.builder.SQLStringBuilder;

/**
 * @author edward
 * @since 2018-09-01
 */
public class DB2Dialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    public String getDialectName() {
        return "db2";
    }

    /**
     * <pre>
     * From DB2 docs https://www.ibm.com/support/knowledgecenter/en/SSEPEK_11.0.0/sqlref/src/tpc/db2z_sql_fetchfirstclause.html
     * ```
     * Specification of the FETCH FIRST clause in an outermost fullselect makes the result table read-only.
     * A read-only result table must not be referenced in an UPDATE, MERGE, or DELETE statement.
     * The FETCH FIRST clause cannot be used in an outermost fullselect that contains a FOR UPDATE clause.
     * ```
     *
     * In short, FETCH FIRST clause can not include FOR UPDATE clause
     *
     * </pre>
     * @param builderCtx BuilderContext from builder
     */
    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        SQLStringBuilder sqlStringBuilder = builderCtx.getSqlStringBuilder();
        if (builderCtx.getOffset() > 0) {
            sqlStringBuilder.prepend("select * from (select sub2_.*, rownumber() over(order by order of sub2_) as _rownumber_ from (",
                            "db2-paging-prefix")
                    .append("fetch first " + builderCtx.getLimit())
                    .append("rows only) as sub2_) as inner1_ where _rownumber_ > " + builderCtx.getOffset()
                            + " order by _rownumber_", "db2-paging-suffix");
        } else {
            sqlStringBuilder.append("fetch first " + builderCtx.getLimit() + " rows only", "db2-paging-suffix");
        }
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
