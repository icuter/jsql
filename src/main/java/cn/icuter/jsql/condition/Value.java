package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-10
 */
public class Value extends AbstractCondition {

    public Value(Object value) {
        super(null, value);
    }

    @Override
    protected Operation assignOp() {
        return Operation.NOOP;
    }

    @Override
    public String toSql() {
        throw new UnsupportedOperationException("toSql does not support in Value Condition");
    }

    @Override
    public String toString() {
        return "value: " + value;
    }
}
