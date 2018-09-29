package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.condition.Var;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.JdbcExecutor;

import java.util.List;
import java.util.Map;

/**
 * @author edward
 * @since 2018-08-05
 */
public interface Builder extends ConditionBuilder {

    Builder select(String... columns);

    Builder from(String... tableName);

    Builder union(Builder builder);

    Builder unionAll(Builder builder);

    Builder distinct();

    Builder groupBy(String... columns);

    Builder outerJoinOn(String tableName, Var var);

    Builder joinOn(String tableName, Var var);

    Builder leftJoinOn(String tableName, Var var);

    Builder rightJoinOn(String tableName, Var var);

    Builder fullJoinOn(String tableName, Var var);

    Builder offset(int offset);

    Builder limit(int limit);

    Builder sql(String sql);

    Builder build();

    String getSql();

    List<Object> getPreparedValues();

    List<Condition> getConditionList();

    BuilderContext getBuilderContext();

    <E> List<E> execQuery(JdbcExecutor executor, Class<E> clazz) throws JSQLException;

    List<Map<String, Object>> execQuery(JdbcExecutor executor) throws JSQLException;

    int execUpdate(JdbcExecutor executor) throws JSQLException;

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
    default <T> Builder values(T value) {
        throw new UnsupportedOperationException();
    }
    // Update Builder
    default Builder update(String tableName) {
        throw new UnsupportedOperationException();
    }
    default Builder set(Eq... eqs) {
        throw new UnsupportedOperationException();
    }
    default <T> Builder set(T value) {
        throw new UnsupportedOperationException();
    }
    // Delete Builder
    default Builder delete() {
        throw new UnsupportedOperationException();
    }
}
