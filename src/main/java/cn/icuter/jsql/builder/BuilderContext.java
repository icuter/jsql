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
//    StringBuilder preparedSql;
    SQLStringBuilder sqlStringBuilder;
    int offset;
    int limit;
//    int forUpdatePosition;
    boolean built;
    boolean hasOrderBy;

    BuilderContext() {
    }

    public void addCondition(Condition condition) {
        throw new UnsupportedOperationException();
    }

    public Dialect getDialect() {
        return dialect;
    }

    /*public StringBuilder getPreparedSql() {
        return preparedSql;
    }*/

    public SQLStringBuilder getSqlStringBuilder() {
        return sqlStringBuilder;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getForUpdatePosition() {
        List<SQLStringBuilder.SQLItem> itemList = sqlStringBuilder.findByType("for-update");
        if (!itemList.isEmpty()) {
            return itemList.get(0).sqlPosition;
        }
        return -1;
    }

    public boolean isBuilt() {
        return built;
    }

    public boolean isHasOrderBy() {
        return hasOrderBy;
    }
}
