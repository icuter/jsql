package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.orm.ORMapper;

import java.util.*;
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
    public Builder insertInto(String tableName) {
        preparedSql.append("insert into ").append(tableName);
        return this;
    }

    private String createPlaceHolder(int placeHolderCnt) {
        return Arrays.stream(new String[placeHolderCnt])
                .map(nvl -> "?")
                .reduce((f, s) -> f + "," + s)
                .orElse("");
    }

    @Override
    public Builder values(Eq... values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException("values must not be null or empty! ");
        }
        preparedSql.append("(")
                .append(Arrays.stream(values).map(Condition::getField).reduce((f, s) -> f + "," + s).orElse(""))
                .append(")")
                .append(" values(").append(createPlaceHolder(values.length)).append(")");
        addCondition(values);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Builder values(T values) {
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
        } else {
            ORMapper mapper = new ORMapper(values);
            List<Eq> eqList = mapper.toMapIgnoreNullValue().entrySet().stream()
                    .map(entry -> Cond.eq(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            return values(eqList.toArray(new Eq[eqList.size()]));
        }
    }
}
