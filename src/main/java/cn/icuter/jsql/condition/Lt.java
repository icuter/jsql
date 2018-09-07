package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-06
 */
public class Lt extends AbstractCondition {
    Lt(String field, Object value) {
        super(field, value);
    }

    @Override
    protected Operation assignOp() {
        return Operation.LT;
    }
}
