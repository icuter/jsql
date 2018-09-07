package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Condition;

import java.util.Collection;

/**
 * @author edward
 * @since 2018-08-08
 */
public interface ConditionBuilder {
    Builder and(Condition condition);

    Builder and(Condition... conditions);

    Builder or(Condition condition);

    Builder or(Condition... conditions);

    Builder where();

    Builder and();

    Builder or();

    Builder exists(Builder builder);

    Builder notExists(Builder builder);

    Builder having(Condition... conditions);

    Builder isNull(String field);

    Builder isNotNull(String field);

    Builder eq(String field, Object value);

    Builder ne(String field, Object value);

    Builder like(String field, Object value);

    Builder ge(String field, Object value);

    Builder gt(String field, Object value);

    Builder le(String field, Object value);

    Builder lt(String field, Object value);

    Builder between(String field, Object start, Object end);

    Builder in(String field, Collection<Object> values);

    Builder in(String field, Object... values);

    Builder in(String field, Builder builder);

    Builder var(String field, String field2);

    Builder value(Object... values);
}
