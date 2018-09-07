package cn.icuter.jsql.condition;

import cn.icuter.jsql.builder.Builder;

import java.util.Collection;

/**
 * @author edward
 * @since 2018-08-05
 */
public abstract class Cond {

    public static Value value(Object value) {
        return new Value(value);
    }

    public static Null isNull(String field) {
        return new Null(field);
    }

    public static NotNull isNotNull(String field) {
        return new NotNull(field);
    }

    public static Var var(String field, String value) {
        return new Var(field, value);
    }

    public static Eq eq(String field, Object value) {
        return new Eq(field, value);
    }

    public static Ne ne(String field, Object value) {
        return new Ne(field, value);
    }

    public static Lt lt(String field, Object value) {
        return new Lt(field, value);
    }

    public static Le le(String field, Object value) {
        return new Le(field, value);
    }

    public static Gt gt(String field, Object value) {
        return new Gt(field, value);
    }

    public static Ge ge(String field, Object value) {
        return new Ge(field, value);
    }

    public static Between between(String field, Object start, Object end) {
        return new Between(field, new Object[]{start, end});
    }

    public static Like like(String field, Object value) {
        return new Like(field, value);
    }

    public static NotLike notLike(String field, Object value) {
        return new NotLike(field, value);
    }

    public static In in(String field, Collection<Object> values) {
        return new In(field, values);
    }

    public static In in(String field, Object... values) {
        return new In(field, values);
    }

    public static In in(String field, Builder builder) {
        return new In(field, builder);
    }

    public static Conditions and(Condition... conditions) {
        return new Conditions(Combination.AND).addCondition(conditions);
    }

    public static Conditions or(Condition... conditions) {
        return new Conditions(Combination.OR).addCondition(conditions);
    }
}
