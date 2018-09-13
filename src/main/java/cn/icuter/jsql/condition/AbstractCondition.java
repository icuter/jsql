package cn.icuter.jsql.condition;

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
        return value;
    }

    @Override
    public int prepareType() {
        return PrepareType.PLACEHOLDER.getType();
    }

    @Override
    public String toSql() {
        return field + " " + op.getSymbol() + (prepareType == PrepareType.PLACEHOLDER.getType() ? " ?" : "");
    }

    @Override
    public String toString() {
        return toSql();
    }
}
