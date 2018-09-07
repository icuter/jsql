package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-09
 */
public class NotNull extends Null {

    NotNull(String field) {
        super(field);
    }

    @Override
    protected Operation assignOp() {
        return Operation.NOT_NULL;
    }
}
