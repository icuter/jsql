package cn.icuter.jsql.condition;

import cn.icuter.jsql.builder.Builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-08-05
 */
public abstract class AbstractCondition implements Condition {

    protected String field;
    protected Operation op;
    protected Object value;
    protected int prepareType;

    public AbstractCondition(String field, Object value) {
        this.field = field;
        this.value = value;
        this.prepareType = prepareType();
        this.op = assignOp();
    }

    protected abstract Operation assignOp();

    @Override
    public String getField() {
        return field;
    }

    public Object getValue() {
        if (value instanceof Builder) {
            Builder builderValue = (Builder) value;
            return builderValue.getPreparedValues();
        }
        return value;
    }

    @Override
    public int prepareType() {
        return PrepareType.PLACEHOLDER.getType();
    }

    @Override
    public String toSql() {
        if (value instanceof Builder) {
            Builder builderValue = (Builder) value;
            return field + " " + op.getSymbol() + " (" + builderValue.getSql() + ")";
        } else if (isMultipleValue()) {
            return createMultipleValueSql();
        }
        return field + " " + op.getSymbol() + (prepareType == PrepareType.PLACEHOLDER.getType() ? " ?" : "");
    }

    private boolean isMultipleValue() {
        return value != null && (Collection.class.isAssignableFrom(value.getClass()) || value.getClass().isArray());
    }

    private String createMultipleValueSql() {
        int placeHolderCnt = 0;
        if (Collection.class.isAssignableFrom(value.getClass())) {
            placeHolderCnt = ((Collection) value).size();
        } else if (value.getClass().isArray()) {
            placeHolderCnt = ((Object[]) value).length;
        }
        String placeHolder = Arrays.stream(new String[placeHolderCnt])
                .map(nvl -> "?")
                .collect(Collectors.joining(","));
        return field + " " + op.getSymbol() + " (" + placeHolder + ")";
    }

    @Override
    public String toString() {
        return toSql();
    }
}
