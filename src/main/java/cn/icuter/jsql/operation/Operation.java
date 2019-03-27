package cn.icuter.jsql.operation;

import cn.icuter.jsql.dialect.Dialect;

import java.util.List;

public class Operation {

    public static final int DML = 0;
    public static final int DQL = 1;
    String buildSql;
    List<Object> preparedValueList;
    Dialect dialect;
    int type;

    public String getWildcardSql() {
        return buildSql;
    }

    public String getOriginSql() {
        //TODO 没有通配的原始sql,会有sql注入的危险
        return "";
    }

    public List<Object> getPreparedValueList() {
        return preparedValueList;
    }

    public int getOperationType() {
        return type;
    }

}
