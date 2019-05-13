package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.builder.SQLStringBuilder;

import java.util.List;

/**
 * @author edward
 * @since 2018-08-30
 */
public class EmbeddedDerbyDialect extends AbstractDialect {
    @Override
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }

    @Override
    public String getDialectName() {
        return "derby";
    }

    /**
     * From Derby Docs:
     * <pre>
     * SELECT statement
     * [ORDER BY clause]
     * [result offset clause]
     * [fetch first clause]
     * [FOR UPDATE clause]
     * [WITH {RR|RS|CS|UR}]
     * </pre>
     *
     * @param builderCtx BuilderContext for more pagination sql combination
     */
    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        StringBuilder offsetLimitBuilder = new StringBuilder();
        boolean offsetExists = builderCtx.getOffset() > 0;
        offsetLimitBuilder.append(offsetExists ? "offset ? rows fetch next ?" : "fetch first ?").append(" rows only");
        if (offsetExists) {
            builderCtx.getBuilder().value(builderCtx.getOffset());
            builderCtx.getBuilder().value(builderCtx.getLimit());
        } else {
            builderCtx.getBuilder().value(builderCtx.getLimit());
        }
        SQLStringBuilder sqlStringBuilder = builderCtx.getSqlStringBuilder();
        int forUpdatePosition = builderCtx.getForUpdatePosition();
        if (forUpdatePosition > 0) {
            sqlStringBuilder.insert(forUpdatePosition, offsetLimitBuilder.toString());
        } else {
            List<SQLStringBuilder.SQLItem> sqlItemList = sqlStringBuilder.findByRegex("with");
            if (!sqlItemList.isEmpty()) {
                SQLStringBuilder.SQLItem item = sqlItemList.get(0);
                sqlStringBuilder.insert(item.getSqlPosition(), offsetLimitBuilder.toString());
            } else {
                sqlStringBuilder.append(offsetLimitBuilder.toString());
            }
        }
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }

    @Override
    public boolean supportNClob() {
        return false;
    }

    @Override
    public boolean requireUserPassword() {
        return false;
    }
}
