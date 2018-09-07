package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-30
 */
public class DerbyDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }

    @Override
    public String getDialectName() {
        return "derby";
    }

    /**
     * <p/>
     * From Derby Docs:
     * <pre>
     * SELECT statement
     * [ORDER BY clause]
     * [result offset clause]
     * [fetch first clause]
     * [FOR UPDATE clause]
     * [WITH {RR|RS|CS|UR}]
     * </pre>
     */
    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        StringBuilder offsetLimitBuilder = new StringBuilder();
        boolean offsetExists = builderCtx.getOffset() > 0;
        offsetLimitBuilder
                .append(offsetExists ? " offset " + (builderCtx.getOffset() + " rows fetch next ") : " fetch first ")
                .append(builderCtx.getLimit()).append(" rows only");

        StringBuilder preparedSqlBuilder = builderCtx.getPreparedSql();
        if (builderCtx.getForUpdatePosition() > 0) {
            preparedSqlBuilder.insert(builderCtx.getForUpdatePosition(), offsetLimitBuilder);
        } else {
            int withIndex = preparedSqlBuilder.toString().toLowerCase().lastIndexOf(" with");
            if (withIndex > 0) {
                preparedSqlBuilder.insert(withIndex, offsetLimitBuilder);
            } else {
                preparedSqlBuilder.append(offsetLimitBuilder);
            }
        }
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }
}
