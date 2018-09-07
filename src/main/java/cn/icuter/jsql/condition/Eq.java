package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-05
 */
public class Eq extends AbstractCondition {

    Eq(String field, Object value) {
        super(field, value);
    }

    @Override
    protected Operation assignOp() {
        return Operation.EQ;
    }
}
