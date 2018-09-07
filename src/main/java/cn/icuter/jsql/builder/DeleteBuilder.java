package cn.icuter.jsql.builder;

import cn.icuter.jsql.dialect.Dialect;

/**
 * @author edward
 * @since 2018-08-05
 */
public class DeleteBuilder extends AbstractBuilder implements DMLBuilder {

    public DeleteBuilder() {

    }
    public DeleteBuilder(Dialect dialect) {
        super(dialect);
    }

    @Override
    public Builder delete() {
        preparedSql.append("delete");
        return this;
    }
}
