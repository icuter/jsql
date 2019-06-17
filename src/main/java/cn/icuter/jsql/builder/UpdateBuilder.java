package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.orm.ORMapper;
import cn.icuter.jsql.security.Injections;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-08-05
 */
public class UpdateBuilder extends AbstractBuilder implements DMLBuilder {

    public UpdateBuilder() {
    }

    public UpdateBuilder(Dialect dialect) {
        super(dialect);
    }

    @Override
    public Builder update(String tableName) {
        Injections.check(tableName, dialect.getQuoteString());
        sqlStringBuilder.append("update").append(tableName);
        return this;
    }

    @Override
    public Builder set(Eq... eqs) {
        if (eqs == null || eqs.length <= 0) {
            throw new IllegalArgumentException("parameters must not be null or empty! ");
        }
        addCondition(eqs);
        String sql = Arrays.stream(eqs)
                .map(Condition::toSql)
                .collect(Collectors.joining(","));
        sqlStringBuilder.append("set").append(sql);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(Object value) {
        Objects.requireNonNull(value, "parameters must not be null");
        if (value instanceof Map) {
            Map<Object, Object> attrs = (Map<Object, Object>) value;
            List<Eq> conditionList = attrs.entrySet().stream()
                    .map(e -> Cond.eq(String.valueOf(e.getKey()), e.getValue()))
                    .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
            return set(conditionList.toArray(new Eq[0]));
        } else if (value instanceof Collection) {
            return set(((Collection<Eq>) value).toArray(new Eq[0]));
        } else if (value instanceof Eq) {
            return set(new Eq[]{(Eq) value});
        } else {
            return setMapAttr(ORMapper.of(value).toMapIgnoreNullValue());
        }
    }

    @Override
    public <T> Builder set(T value, FieldInterceptor<T> interceptor) {
        return setMapAttr(ORMapper.of(value).toMap(interceptor));
    }

    private Builder setMapAttr(Map<String, Object> attrs) {
        List<Eq> conditionList = attrs.entrySet().stream()
                .map(e -> Cond.eq(e.getKey(), e.getValue()))
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
        return set(conditionList.toArray(new Eq[0]));
    }
}
