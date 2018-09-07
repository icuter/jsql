package cn.icuter.jsql.builder;

import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.dialect.Dialect;

import java.util.List;


/**
 * @author edward
 * @since 2018-08-30
 */
public class BuilderContext {

    Dialect dialect;
    List<Condition> conditionList;
    StringBuilder preparedSql;
    int offset;
    int limit;
    int forUpdatePosition;
    boolean built;
    boolean hasOrderBy;

    BuilderContext() {
    }

    public List<Condition> getConditionList() {
        return conditionList;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public StringBuilder getPreparedSql() {
        return preparedSql;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getForUpdatePosition() {
        return forUpdatePosition;
    }

    public boolean isBuilt() {
        return built;
    }

    public boolean isHasOrderBy() {
        return hasOrderBy;
    }
}
