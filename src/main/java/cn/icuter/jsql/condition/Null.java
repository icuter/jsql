package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-09
 */
public class Null extends AbstractCondition {

    Null(String field) {
        super(field, null);
    }

    @Override
    protected Operation assignOp() {
        return Operation.NULL;
    }

    @Override
    public int prepareType() {
        return PrepareType.IGNORE.getType();
    }
}
