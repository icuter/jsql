package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-06
 */
public class Ge extends AbstractCondition {

    Ge(String field, Object value) {
        super(field, value);
    }

    @Override
    protected Operation assignOp() {
        return Operation.GE;
    }
}
