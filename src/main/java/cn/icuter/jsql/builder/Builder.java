package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;

import java.util.List;

/**
 * @author edward
 * @since 2018-08-05
 */
public interface Builder extends ConditionBuilder, ExecutableBuilder {

    Builder select(String... columns);

    Builder from(String... tableName);

    Builder distinct();

    Builder groupBy(String... columns);

    Builder outerJoinOn(String tableName, Condition... conditions);

    Builder joinOn(String tableName, Condition... conditions);

    Builder leftJoinOn(String tableName, Condition... conditions);

    Builder rightJoinOn(String tableName, Condition... conditions);

    Builder fullJoinOn(String tableName, Condition... conditions);

    Builder offset(int offset);

    Builder limit(int limit);

    Builder sql(String sql);

    Builder build();

    String getSql();

    List<Object> getPreparedValues();

    List<Condition> getConditionList();

    BuilderContext getBuilderContext();

    // Select Builder
    Builder orderBy(String... columns);
    Builder forUpdate(String... columns);
    // Insert Builder
    Builder insert(String tableName);
    Builder values(Eq... values);
    Builder values(Object value);
    <T> Builder values(T value, FieldInterceptor<T> interceptor);
    // Update Builder
    Builder update(String tableName);
    Builder set(Eq... eqs);
    Builder set(Object value);
    <T> Builder set(T value, FieldInterceptor<T> interceptor);
    // Delete Builder
    Builder delete();
    // Union Select Builder
    Builder union(Builder builder);
    Builder unionAll(Builder builder);
}
