package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.orm.ORMapper;
import cn.icuter.jsql.util.CollectionUtil;
import cn.icuter.jsql.util.ObjectUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    public Builder insert(String tableName, String... columns) {
        sqlStringBuilder.append("insert into").append(tableName);
        if (columns != null && columns.length > 0) {
            sqlStringBuilder.append("(" + CollectionUtil.join(columns, ",") + ")");
        }
        return this;
    }

    @Override
    public Builder values(List<Object> values) {
        if (values == null || values.size() <= 0) {
            throw new IllegalArgumentException("values must not be null or empty! ");
        }
        sqlStringBuilder.append("values(" + createPlaceHolder(values.size()) + ")");
        return value(values.toArray());
    }

    @Override
    public Builder values(Eq... values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException("values must not be null or empty! ");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            builder.append(values[i].getField());
            if (i != values.length - 1) {
                builder.append(",");
            }
        }
        sqlStringBuilder
                .append("(" + builder.toString() + ")")
                .append("values(" + createPlaceHolder(values.length) + ")");
        addCondition(values);
        return this;
    }

    private String createPlaceHolder(int placeHolderCnt) {
        String[] placeHolders = new String[placeHolderCnt];
        Arrays.fill(placeHolders, "?");
        return CollectionUtil.join(placeHolders, ",");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder values(Object values) {
        ObjectUtil.requireNonNull(values, "values must not be null");
        if (values instanceof Map) {
            Map<Object, Object> attrs = (Map<Object, Object>) values;
            Eq[] eqs = new Eq[attrs.size()];
            int i = 0;
            for (Map.Entry<Object, Object> entry : attrs.entrySet()) {
                eqs[i++] = Cond.eq(String.valueOf(entry.getKey()), entry.getValue());
            }
            return values(eqs);
        } else if (values instanceof Collection) {
            return values(((Collection<Eq>) values).toArray(new Eq[0]));
        } else if (values instanceof Eq) {
            return values(new Eq[]{(Eq) values});
        } else if (values.getClass().isArray()) {
            if (Eq.class.isAssignableFrom(values.getClass().getComponentType())) {
                return values((Eq[]) values); // never be here, using values(Eq...) instead, just in case
            } else {
                return values(Arrays.asList((Object[]) values));
            }
        } else {
            return valuesMap(ORMapper.of(values).toMapIgnoreNullValue());
        }
    }

    @Override
    public <T> Builder values(T value, FieldInterceptor<T> interceptor) {
        return valuesMap(ORMapper.of(value).toMap(interceptor));
    }

    private Builder valuesMap(Map<String, Object> attrs) {
        Eq[] eqs = new Eq[attrs.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            eqs[i++] = Cond.eq(entry.getKey(), entry.getValue());
        }
        return values(eqs);
    }
}
