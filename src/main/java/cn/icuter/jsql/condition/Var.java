package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-08
 */
public class Var extends AbstractCondition {

    Var(String field, String value) {
        super(field, value);
    }

    @Override
    protected Operation assignOp() {
        return Operation.EQ;
    }

    @Override
    public String toSql() {
        return field + op.getSymbol() + value;
    }

    @Override
    public int prepareType() {
        return PrepareType.PLAIN.getType();
    }
}
