package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

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
        StringBuilder preparedSqlBuilder = builderCtx.getPreparedSql();
        if (builderCtx.getOffset() > 0) {
            preparedSqlBuilder.insert(0, "select * from ( select sub2_.*, rownumber() over(order by order of sub2_) as _rownumber_ from ( ");
            preparedSqlBuilder.append(" fetch first ").append(builderCtx.getLimit()).append(" rows only ) as sub2_ ) as inner1_ where _rownumber_ > ")
                    .append(builderCtx.getOffset()).append(" order by _rownumber_");
        } else {
            preparedSqlBuilder.append(" fetch first ").append(builderCtx.getLimit()).append(" rows only");
        }
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
