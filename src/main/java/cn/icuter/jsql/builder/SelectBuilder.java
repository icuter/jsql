package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.PrepareType;
import cn.icuter.jsql.dialect.Dialect;

/**
 * @author edward
 * @since 2018-08-05
 */
public class SelectBuilder extends AbstractBuilder implements DQLBuilder {

    public SelectBuilder() {

    }

    public SelectBuilder(Dialect dialect) {
        super(dialect);
    }

    @Override
    public Builder forUpdate(String... columns) {
        sqlStringBuilder.append("for update", "for-update");
        if (columns != null && columns.length > 0) {
            sqlStringBuilder.append("of").append(String.join(",", columns));
        }
        return this;
    }

    @Override
    public Builder orderBy(String... columns) {
        if (columns == null || columns.length <= 0) {
            throw new IllegalArgumentException("must define [order by] columns!");
        }
        sqlStringBuilder.append("order by", "order-by")
                .append(String.join(",", columns));
        builderContext.hasOrderBy = true;
        return this;
    }

    @Override
    public String toSql() {
        return getSql();
    }

    @Override
    public String getField() {
        return null;
    }

    @Override
    public Object getValue() {
        return getPreparedValues();
    }

    @Override
    public int prepareType() {
        return PrepareType.PLACEHOLDER.getType();
    }
}
