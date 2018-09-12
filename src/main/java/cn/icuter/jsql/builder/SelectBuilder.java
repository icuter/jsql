package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.PrepareType;
import cn.icuter.jsql.dialect.Dialect;

import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
        builderContext.forUpdatePosition = preparedSql.length();
        preparedSql.append(" for update");
        if (columns != null && columns.length > 0) {
            preparedSql.append(" of ").append(Arrays.stream(columns).collect(Collectors.joining(",")));
        }
        return this;
    }

    @Override
    public Builder orderBy(String... columns) {
        if (columns == null || columns.length <= 0) {
            throw new IllegalArgumentException("must define [order by] columns!");
        }
        preparedSql.append(" order by ").append(Arrays.stream(columns).collect(Collectors.joining(",")));
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
