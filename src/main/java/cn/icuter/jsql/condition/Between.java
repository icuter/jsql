package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-06
 */
public class Between extends AbstractCondition {

    Between(String field, Object value) {
        super(field, value);
    }

    @Override
    protected Operation assignOp() {
        return Operation.BETWEEN;
    }

    @Override
    public String toSql() {
        return " " + field + " " + op.getSymbol() + " ? and ?";
    }
}
