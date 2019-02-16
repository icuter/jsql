package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.orm.ORMapper;

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
public class InsertBuilder extends AbstractBuilder implements DMLBuilder {

    public InsertBuilder() {
    }

    public InsertBuilder(Dialect dialect) {
        super(dialect);
    }

    @Override
    public Builder insert(String tableName) {
        sqlStringBuilder.append("insert into").append(tableName);
        return this;
    }

    @Override
    public Builder values(Eq... values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException("values must not be null or empty! ");
        }
        sqlStringBuilder
                .append("(" + Arrays.stream(values).map(Condition::getField).collect(Collectors.joining(",")) + ")")
                .append("values(" + createPlaceHolder(values.length) + ")");
        addCondition(values);
        return this;
    }

    private String createPlaceHolder(int placeHolderCnt) {
        return Arrays.stream(new String[placeHolderCnt])
                .map(nvl -> "?")
                .collect(Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder values(Object values) {
        Objects.requireNonNull(values, "values must not be null");

        if (values instanceof Map) {
            Map<Object, Object> attrs = (Map<Object, Object>) values;
            List<Eq> conditionList = attrs.entrySet().stream()
                    .map(e -> Cond.eq(e.getKey().toString(), e.getValue()))
                    .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
            return values(conditionList.toArray(new Eq[conditionList.size()]));
        } else if (values instanceof Collection) {
            Collection<Eq> eqs = (Collection<Eq>) values;
            return values(eqs.toArray(new Eq[eqs.size()]));
        } else if (values instanceof Eq) {
            return values(new Eq[]{(Eq) values});
        } else {
            Map<String, Object> attrs = ORMapper.of(values).toMapIgnoreNullValue();
            return valuesMap(attrs);
        }
    }

    @Override
    public <T> Builder values(T value, FieldInterceptor<T> interceptor) {
        Map<String, Object> attrs = ORMapper.of(value).toMap(interceptor);
        return valuesMap(attrs);
    }

    private Builder valuesMap(Map<String, Object> attrs) {
        List<Eq> eqList = attrs.entrySet().stream()
                .map(e -> Cond.eq(e.getKey(), e.getValue()))
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
        return values(eqList.toArray(new Eq[eqList.size()]));
    }
}
