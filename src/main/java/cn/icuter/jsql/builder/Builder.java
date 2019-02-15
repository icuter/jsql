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
    default Builder orderBy(String... columns) {
        throw new UnsupportedOperationException();
    }
    default Builder forUpdate(String... columns) {
        throw new UnsupportedOperationException();
    }
    // Insert Builder
    default Builder insert(String tableName) {
        throw new UnsupportedOperationException();
    }
    default Builder values(Eq... values) {
        throw new UnsupportedOperationException();
    }
    default Builder values(Object value) {
        throw new UnsupportedOperationException();
    }
    default <T> Builder values(T value, FieldInterceptor<T> interceptor) {
        throw new UnsupportedOperationException();
    }
    // Update Builder
    default Builder update(String tableName) {
        throw new UnsupportedOperationException();
    }
    default Builder set(Eq... eqs) {
        throw new UnsupportedOperationException();
    }
    default Builder set(Object value) {
        throw new UnsupportedOperationException();
    }
    default <T> Builder set(T value, FieldInterceptor<T> interceptor) {
        throw new UnsupportedOperationException();
    }
    // Delete Builder
    default Builder delete() {
        throw new UnsupportedOperationException();
    }
    // Union Select Builder
    default Builder union(Builder builder) {
        throw new UnsupportedOperationException();
    }
    default Builder unionAll(Builder builder) {
        throw new UnsupportedOperationException();
    }
}
