package cn.icuter.jsql.condition;

import cn.icuter.jsql.builder.Builder;

import java.util.Collection;

/**
 * @author edward
 * @since 2018-08-06
 */
public class In extends AbstractCondition {

    In(String field, Object value) {
        super(field, value);
        checkListOrArray(value);
    }

    In(String field, Builder value) {
        super(field, value);
        checkIsBuilderValue(value);
    }

    private void checkListOrArray(Object value) {
        Class<?> valClass = value.getClass();
        if (!Collection.class.isAssignableFrom(valClass) && !valClass.isArray()) {
            throw new IllegalArgumentException("value must be a collection or array! ");
        }
    }

    private boolean checkIsBuilderValue(Object value) {
        return Builder.class.isAssignableFrom(value.getClass());
    }

    @Override
    protected Operation assignOp() {
        return Operation.IN;
    }
}
