package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.PrepareType;
import cn.icuter.jsql.condition.Var;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-08-07
 */
public abstract class AbstractBuilder implements Builder {

    private boolean isDistinct;
    private String buildSql;
    protected BuilderContext builderContext;

    StringBuilder preparedSql = new StringBuilder();
    protected List<Condition> conditionList = new LinkedList<>();
    private List<Object> preparedValueList;
    private int offset;
    private int limit;
    private Dialect dialect;

    public AbstractBuilder() {
        this(Dialects.UNKNOWN);
    }

    public AbstractBuilder(Dialect dialect) {
        this.dialect = dialect;
        init();
    }

    public void init() {
        builderContext = new BuilderContext();
        builderContext.preparedSql = preparedSql;
        builderContext.dialect = dialect;
        builderContext.conditionList = conditionList;
        builderContext.offset = offset;
        builderContext.limit = limit;
    }

    @Override
    public Builder select(String... columns) {
        String columnStr = "*";
        if (columns != null && columns.length > 0) {
            columnStr = Arrays.stream(columns).reduce((f, s) -> f + ", " + s).orElse("*");
        }
        preparedSql.append("select %s").append(columnStr);
        return this;
    }

    @Override
    public Builder from(String... tableName) {
        preparedSql.append(" from ").append(Arrays.stream(tableName).reduce((f, s) -> f + ", " + s).orElse(""));
        return this;
    }

    @Override
    public Builder union(Builder builder) {
        preparedSql.append(" union ").append(builder.getSql());
        conditionList.addAll(builder.getConditionList());
        return this;
    }

    @Override
    public Builder unionAll(Builder builder) {
        preparedSql.append(" union all ").append(builder.getSql());
        conditionList.addAll(builder.getConditionList());
        return this;
    }

    @Override
    public Builder distinct() {
        isDistinct = true;
        return this;
    }

    @Override
    public Builder and(Condition condition) {
        addCondition(condition);
        preparedSql.append(" and").append(condition.toSql());
        return this;
    }

    @Override
    public Builder and(Condition... conditions) {
        addCondition(conditions);
        preparedSql.append(Cond.and(conditions).toSql());
        return this;
    }

    @Override
    public Builder or(Condition condition) {
        addCondition(condition);
        preparedSql.append(" or").append(condition.toSql());
        return this;
    }

    @Override
    public Builder or(Condition... conditions) {
        addCondition(conditions);
        preparedSql.append(Cond.or(conditions).toSql());
        return this;
    }

    @Override
    public Builder where() {
        preparedSql.append(" where");
        return this;
    }

    @Override
    public Builder groupBy(String... columns) {
        if (columns == null || columns.length <= 0) {
            throw new IllegalArgumentException("columns must not be null or empty! ");
        }
        String columnStr = Arrays.stream(columns).reduce((f, s) -> f + "," + s).orElse("");
        preparedSql.append(" group by ").append(columnStr);
        return this;
    }

    @Override
    public Builder having(Condition... conditions) {
        addCondition(conditions);
        preparedSql.append(" having").append(Cond.and(conditions).toSql());
        return this;
    }

    @Override
    public Builder outerJoinOn(String tableName, Var var) {
        addCondition(var);
        preparedSql.append(" outer join ").append(tableName).append(" on").append(var.toSql());
        return this;
    }

    @Override
    public Builder joinOn(String tableName, Var var) {
        addCondition(var);
        preparedSql.append(" join ").append(tableName).append(" on").append(var.toSql());
        return this;
    }

    @Override
    public Builder leftJoinOn(String tableName, Var var) {
        addCondition(var);
        preparedSql.append(" left join ").append(tableName).append(" on").append(var.toSql());
        return this;
    }

    @Override
    public Builder rightJoinOn(String tableName, Var var) {
        addCondition(var);
        preparedSql.append(" right join ").append(tableName).append(" on").append(var.toSql());
        return this;
    }

    @Override
    public Builder fullJoinOn(String tableName, Var var) {
        addCondition(var);
        preparedSql.append(" full join ").append(tableName).append(" on").append(var.toSql());
        return this;
    }

    @Override
    public Builder offset(int offset) {
        this.offset = offset;
        builderContext.offset = offset;
        return this;
    }

    @Override
    public Builder limit(int limit) {
        this.limit = limit;
        builderContext.limit = limit;
        return this;
    }

    @Override
    public Builder sql(String sql) {
        preparedSql.append(preparedSql.length() > 0 ? " " : "").append(sql);
        return this;
    }

    @Override
    public Builder build() {
        if (builderContext.built) {
            throw new IllegalStateException("Builder has been built");
        }
        if (dialect.supportOffsetLimit() && ((offset > 0 && limit > 0) || limit > 0)) {
            dialect.injectOffsetLimit(builderContext);
        }
        buildSql = String.format(preparedSql.toString(), isDistinct ? "distinct " : "");
        preparedValueList = conditionList.stream()
                .filter(condition -> condition.prepareType() == PrepareType.PLACEHOLDER.getType())
                .map(Condition::getValue)
                .collect(LinkedList::new,
                        (list, condValue) -> {
                            if (condValue == null) {
                                list.add(null);
                            } else if (condValue.getClass().isArray()) {
                                Object[] values = (Object[]) condValue;
                                list.addAll(Arrays.asList(values));
                            } else if (condValue instanceof Collection) {
                                list.addAll((Collection) condValue);
                            } else {
                                list.add(condValue);
                            }
                        }, LinkedList::addAll);
        builderContext.built = true;
        return this;
    }

    @Override
    public String getSql() {
        return buildSql;
    }

    @Override
    public List<Object> getPreparedValues() {
        return preparedValueList;
    }

    @Override
    public List<Condition> getConditionList() {
        return conditionList;
    }

    @Override
    public BuilderContext getBuilderContext() {
        return builderContext;
    }

    @Override
    public Builder and() {
        preparedSql.append(" and");
        return this;
    }

    @Override
    public Builder or() {
        preparedSql.append(" or");
        return this;
    }

    @Override
    public Builder exists(Builder builder) {
        preparedSql.append(" exists (").append(builder.getSql()).append(")");
        conditionList.addAll(builder.getConditionList());
        return this;
    }

    @Override
    public Builder notExists(Builder builder) {
        preparedSql.append(" not exists (").append(builder.getSql()).append(")");
        conditionList.addAll(builder.getConditionList());
        return this;
    }

    @Override
    public Builder isNull(String field) {
        Condition condition = Cond.isNull(field);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder isNotNull(String field) {
        Condition condition = Cond.isNotNull(field);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder eq(String field, Object value) {
        Condition condition = Cond.eq(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder ne(String field, Object value) {
        Condition condition = Cond.ne(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder like(String field, Object value) {
        Condition condition = Cond.like(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder ge(String field, Object value) {
        Condition condition = Cond.ge(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder gt(String field, Object value) {
        Condition condition = Cond.gt(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder le(String field, Object value) {
        Condition condition = Cond.le(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder lt(String field, Object value) {
        Condition condition = Cond.lt(field, value);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder between(String field, Object start, Object end) {
        // [BETWEEN .. AND ..] clause unsupport DQLBuilder as value
        Condition condition = Cond.between(field, start, end);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder in(String field, Collection<Object> values) {
        Condition condition = Cond.in(field, values);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder in(String field, Object... values) {
        Condition condition = Cond.in(field, values);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder in(String field, Builder builder) {
        Condition condition = Cond.in(field, builder);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder var(String field, String field2) {
        Condition condition = Cond.var(field, field2);
        addCondition(condition);
        preparedSql.append(condition.toSql());
        return this;
    }

    @Override
    public Builder value(Object... values) {
        Objects.requireNonNull(values, "values must not be null");
        conditionList.addAll(Arrays.stream(values).map(Cond::value).collect(Collectors.toList()));
        return this;
    }

    protected void addCondition(Condition... conditions) {
        if (conditions != null && conditions.length > 0) {
            conditionList.addAll(Arrays.asList(conditions));
        }
    }
}
