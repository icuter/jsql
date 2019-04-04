package cn.icuter.jsql.builder;

import cn.icuter.jsql.dialect.Dialect;

import java.util.List;


/**
 * @author edward
 * @since 2018-08-30
 */
public class BuilderContext {

    Dialect dialect;
    SQLStringBuilder sqlStringBuilder;
    int offset;
    int limit;
    boolean built;
    boolean hasOrderBy;
    int sqlLevel;
    Builder builder;

    BuilderContext() {
    }

    public Dialect getDialect() {
        return dialect;
    }

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

    public int getSqlLevel() {
        return sqlLevel;
    }

    public Builder getBuilder() {
        return builder;
    }
}
