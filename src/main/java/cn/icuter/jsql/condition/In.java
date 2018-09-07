package cn.icuter.jsql.condition;

import cn.icuter.jsql.builder.Builder;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author edward
 * @since 2018-08-06
 */
public class In extends AbstractCondition {

    private boolean builderValue;

    In(String field, Object value) {
        super(field, value);
        checkListOrArray(value);
    }

    In(String field, Builder value) {
        super(field, value);
        builderValue = true;
    }

    private void checkListOrArray(Object value) {
        Class<?> valClass = value.getClass();
        if (!Collection.class.isAssignableFrom(valClass) && !valClass.isArray()) {
            throw new IllegalArgumentException("Value must be a collection or array! ");
        }
    }

    @Override
    protected Operation assignOp() {
        return Operation.IN;
    }

    @Override
    public String toSql() {
        if (builderValue) {
            Builder builder = (Builder) value;
            return " " + field + " " + op.getSymbol() + " (" + builder.getSql() + ")";
        }
        int placeHolderCnt = 0;
        if (Collection.class.isAssignableFrom(value.getClass())) {
            placeHolderCnt = ((Collection) value).size();
        } else if (value.getClass().isArray()){
            placeHolderCnt = ((Object[]) value).length;
        }
        String placeHolder = Arrays.stream(new String[placeHolderCnt])
                .map(nvl -> "?")
                .reduce((f, s) -> f + "," + s)
                .orElse("");
        return " " + field + " " + op.getSymbol() + " (" + placeHolder + ")";
    }

    @Override
    public Object getValue() {
        if (builderValue) {
            return ((Builder) value).getPreparedValues();
        }
        return super.getValue();
    }
}
