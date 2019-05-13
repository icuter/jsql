package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.condition.PrepareType;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;
import cn.icuter.jsql.exception.ExecutionException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.util.CollectionUtil;
import cn.icuter.jsql.util.ObjectUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author edward
 * @since 2018-08-07
 */
public class AbstractBuilder implements Builder {

    private String buildSql;
    protected BuilderContext builderContext;

    SQLStringBuilder sqlStringBuilder = new SQLStringBuilder();
    protected List<Condition> conditionList = new LinkedList<Condition>();
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
        builderContext.sqlStringBuilder = sqlStringBuilder;
        builderContext.dialect = dialect;
        builderContext.offset = offset;
        builderContext.limit = limit;
        builderContext.builder = this;
    }

    @Override
    public Builder select(String... columns) {
        String columnStr = "*";
        if (columns != null && columns.length > 0) {
            columnStr = CollectionUtil.join(columns, ", ");
        }
        boolean existsTopSelect = sqlStringBuilder.existsType("top-select");
        sqlStringBuilder.append("select", existsTopSelect ? "sub-select" : "top-select")
                        .append(columnStr, existsTopSelect ? "sub-columns" : "top-columns");
        return this;
    }

    @Override
    public Builder from(String... tableName) {
        sqlStringBuilder.append("from").append(CollectionUtil.join(tableName, ","));
        return this;
    }

    @Override
    public Builder distinct() {
        List<SQLStringBuilder.SQLItem> itemList = sqlStringBuilder.findByType("top-select");
        if (!itemList.isEmpty()) {
            sqlStringBuilder.insert(itemList.get(0).sqlPosition + 1, "distinct");
        }
        return this;
    }

    @Override
    public Builder and(Condition condition) {
        addCondition(condition);
        sqlStringBuilder.append("and").append(condition.toSql());
        return this;
    }

    @Override
    public Builder and(Condition... conditions) {
        Condition andConditions = Cond.and(conditions);
        addCondition(andConditions);
        sqlStringBuilder.append(andConditions.toSql());
        return this;
    }

    @Override
    public Builder and(List<Condition> conditionList) {
        Condition andConditions = Cond.and(conditionList);
        addCondition(andConditions);
        sqlStringBuilder.append(andConditions.toSql());
        return this;
    }

    @Override
    public Builder or(Condition condition) {
        addCondition(condition);
        sqlStringBuilder.append("or").append(condition.toSql());
        return this;
    }

    @Override
    public Builder or(Condition... conditions) {
        Condition orConditions = Cond.or(conditions);
        addCondition(orConditions);
        sqlStringBuilder.append(orConditions.toSql());
        return this;
    }

    @Override
    public Builder or(List<Condition> conditionList) {
        Condition orConditions = Cond.or(conditionList);
        addCondition(orConditions);
        sqlStringBuilder.append(orConditions.toSql());
        return this;
    }

    @Override
    public Builder where() {
        sqlStringBuilder.append("where", "where-conditions");
        return this;
    }

    @Override
    public Builder groupBy(String... columns) {
        if (columns == null || columns.length <= 0) {
            throw new IllegalArgumentException("columns must not be null or empty! ");
        }
        String columnStr = CollectionUtil.join(columns, ",");
        sqlStringBuilder.append("group by").append(columnStr);
        return this;
    }

    @Override
    public Builder having(Condition... conditions) {
        addCondition(conditions);
        sqlStringBuilder.append("having").append(Cond.and(conditions).toSql());
        return this;
    }

    @Override
    public Builder joinOn(String tableName, Condition... conditions) {
        joinOn("join", tableName, conditions);
        return this;
    }
    @Override
    public Builder joinUsing(String tableName, String... columns) {
        joinUsing("join", tableName, columns);
        return this;
    }

    @Override
    public Builder leftJoinOn(String tableName, Condition... conditions) {
        joinOn("left join", tableName, conditions);
        return this;
    }
    @Override
    public Builder leftJoinUsing(String tableName, String... columns) {
        joinUsing("left join", tableName, columns);
        return this;
    }

    @Override
    public Builder rightJoinOn(String tableName, Condition... conditions) {
        joinOn("right join", tableName, conditions);
        return this;
    }
    @Override
    public Builder rightJoinUsing(String tableName, String... columns) {
        joinUsing("right join", tableName, columns);
        return this;
    }

    @Override
    public Builder fullJoinOn(String tableName, Condition... conditions) {
        joinOn("full join", tableName, conditions);
        return this;
    }
    @Override
    public Builder fullJoinUsing(String tableName, String... columns) {
        joinUsing("full join", tableName, columns);
        return this;
    }

    private void joinOn(String keyword, String tableName, Condition... conditions) {
        Condition condition = Cond.and(conditions);
        addCondition(condition);
        sqlStringBuilder.append(keyword).append(tableName).append("on").append(condition.toSql());
    }

    private void joinUsing(String keyword, String tableName, String... columns) {
        if (columns == null || columns.length <= 0) {
            throw new IllegalArgumentException("No column defined in USING condition !");
        }
        sqlStringBuilder.append(keyword).append(tableName)
                .append("using (")
                .append(CollectionUtil.join(columns, ","))
                .append(")");
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
        sqlStringBuilder.append(sql);
        return this;
    }

    @Override
    public Builder build() {
        if (builderContext.hasBuilt()) {
            return this;
        }
        if (dialect.supportOffsetLimit() && limit > 0) {
            dialect.injectOffsetLimit(builderContext);
        }
        buildSql = sqlStringBuilder.serialize();
        preparedValueList = new LinkedList<Object>();
        for (Condition condition : conditionList) {
            if (condition.prepareType() == PrepareType.PLACEHOLDER.getType()) {
                addPreparedValue(preparedValueList, condition.getValue());
            }
        }
        return this;
    }

    private void addPreparedValue(List<Object> list, Object condValue) {
        if (condValue == null) {
            list.add(null);
        } else if (condValue.getClass().isArray() && !(condValue instanceof byte[])) {
            for (Object v : (Object[]) condValue) {
                addPreparedValue(list, v);
            }
        } else if (condValue instanceof Collection) {
            for (Object v : (Collection) condValue) {
                addPreparedValue(list, v);
            }
        } else {
            list.add(condValue);
        }
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
    public Builder orderBy(String... columns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder forUpdate(String... columns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder insert(String tableName, String... columns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder values(List<Object> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder values(Eq... values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder values(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Builder values(T value, FieldInterceptor<T> interceptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder update(String tableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder set(Eq... eqs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder set(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Builder set(T value, FieldInterceptor<T> interceptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder union(Builder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder unionAll(Builder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder and() {
        sqlStringBuilder.append("and");
        return this;
    }

    @Override
    public Builder or() {
        sqlStringBuilder.append("or");
        return this;
    }

    @Override
    public Builder exists(Builder builder) {
        sqlStringBuilder.append("exists").append("(" + builder.getSql() + ")", "exists");
        addCondition(builder.getConditionList());
        return this;
    }

    @Override
    public Builder notExists(Builder builder) {
        sqlStringBuilder.append("not exists").append("(" + builder.getSql() + ")", "not-exists");
        addCondition(builder.getConditionList());
        return this;
    }

    @Override
    public Builder isNull(String field) {
        Condition condition = Cond.isNull(field);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder isNotNull(String field) {
        Condition condition = Cond.isNotNull(field);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder eq(String field, Object value) {
        Condition condition = Cond.eq(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder ne(String field, Object value) {
        Condition condition = Cond.ne(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder like(String field, Object value) {
        Condition condition = Cond.like(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder ge(String field, Object value) {
        Condition condition = Cond.ge(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder gt(String field, Object value) {
        Condition condition = Cond.gt(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder le(String field, Object value) {
        Condition condition = Cond.le(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder lt(String field, Object value) {
        Condition condition = Cond.lt(field, value);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder between(String field, Object start, Object end) {
        Condition condition = Cond.between(field, start, end);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder in(String field, Collection<Object> values) {
        Condition condition = Cond.in(field, values);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder in(String field, Object... values) {
        Condition condition = Cond.in(field, values);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder in(String field, Builder builder) {
        Condition condition = Cond.in(field, builder);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder var(String field, String field2) {
        Condition condition = Cond.var(field, field2);
        addCondition(condition);
        sqlStringBuilder.append(condition.toSql());
        return this;
    }

    @Override
    public Builder value(Object... values) {
        ObjectUtil.requireNonNull(values, "values must not be null");
        for (Object val : values) {
            conditionList.add(Cond.value(val));
        }
        return this;
    }

    @Override
    public int execUpdate(JdbcExecutor executor) throws JSQLException {
        if (!(this instanceof DMLBuilder) && !(this instanceof SQLBuilder)) {
            throw new ExecutionException("class of " + this.getClass().getName() + " do not allow execUpdate");
        }
        if (!builderContext.hasBuilt()) {
            build();
        }
        return executor.execUpdate(this);
    }

    @Override
    public <E> List<E> execQuery(JdbcExecutor executor, Class<E> clazz) throws JSQLException {
        if (!(this instanceof DQLBuilder) && !(this instanceof SQLBuilder)) {
            throw new ExecutionException("class of " + this.getClass().getName() + " do not allow execQuery");
        }
        if (!builderContext.hasBuilt()) {
            build();
        }
        return executor.execQuery(this, clazz);
    }

    @Override
    public List<Map<String, Object>> execQuery(JdbcExecutor executor) throws JSQLException {
        if (!(this instanceof DQLBuilder) && !(this instanceof SQLBuilder)) {
            throw new ExecutionException("class of " + this.getClass().getName() + " do not allow execQuery");
        }
        if (!builderContext.hasBuilt()) {
            build();
        }
        return executor.execQuery(this);
    }

    @Override
    public int execUpdate() throws JSQLException {
        JdbcExecutor jdbcExecutor = provideClosableExecutor();
        try {
            try {
                return execUpdate(jdbcExecutor);
            } finally {
                jdbcExecutor.close();
            }
        } catch (IOException e) {
            throw new JSQLException(e);
        }
    }
    @Override
    public <E> List<E> execQuery(Class<E> clazz) throws JSQLException {
        JdbcExecutor jdbcExecutor = provideClosableExecutor();
        try {
            try {
                return execQuery(jdbcExecutor, clazz);
            } finally {
                jdbcExecutor.close();
            }
        } catch (IOException e) {
            throw new JSQLException(e);
        }
    }
    @Override
    public List<Map<String, Object>> execQuery() throws JSQLException {
        JdbcExecutor jdbcExecutor = provideClosableExecutor();
        try {
            try {
                return execQuery(jdbcExecutor);
            } finally {
                jdbcExecutor.close();
            }
        } catch (IOException e) {
            throw new JSQLException(e);
        }
    }

    protected JdbcExecutor provideClosableExecutor() {
        throw new UnsupportedOperationException();
    }

    protected void addCondition(Condition... conditions) {
        if (conditions != null && conditions.length > 0) {
            conditionList.addAll(Arrays.asList(conditions));
        }
    }

    protected void addCondition(Collection<Condition> conditions) {
        if (conditions != null && !conditions.isEmpty()) {
            conditionList.addAll(conditions);
        }
    }

    @Override
    public String toString() {
        return builderContext.hasBuilt() ? ("sql: " + buildSql + ", values:" + preparedValueList) : "Builder not build yet";
    }
}
