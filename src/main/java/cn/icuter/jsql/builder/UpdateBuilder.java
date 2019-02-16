package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.orm.ORMapper;
import cn.icuter.jsql.util.ObjectUtil;

import java.util.Collection;
import java.util.Map;

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
        sqlStringBuilder.append("update").append(tableName);
        return this;
    }

    @Override
    public Builder set(Eq... eqs) {
        if (eqs == null || eqs.length <= 0) {
            throw new IllegalArgumentException("parameters must not be null or empty! ");
        }
        StringBuilder builder = new StringBuilder();
        for (Eq eq : eqs) {
            builder.append(eq.toSql()).append(",");
        }
        sqlStringBuilder.append("set").append(builder.toString().replaceFirst(",\\s*$", ""));
        addCondition(eqs);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(Object value) {
        ObjectUtil.requireNonNull(value, "parameters must not be null");
        if (value instanceof Map) {
            Map<Object, Object> attrs = (Map<Object, Object>) value;
            Eq[] eqs = new Eq[attrs.size()];
            int i = 0;
            for (Map.Entry entry : attrs.entrySet()) {
                eqs[i++] = Cond.eq(String.valueOf(entry.getKey()), entry.getValue());
            }
            return set(eqs);
        } else if (value instanceof Collection) {
            Collection<Eq> eqs = (Collection<Eq>) value;
            return set(eqs.toArray(new Eq[eqs.size()]));
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
        Eq[] eqs = new Eq[attrs.size()];
        int i = 0;
        for (Map.Entry entry : attrs.entrySet()) {
            eqs[i++] = Cond.eq(String.valueOf(entry.getKey()), entry.getValue());
        }
        return set(eqs);
    }
}
