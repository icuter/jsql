package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-05
 */
public interface Condition {
    String toSql();
    String getField();
    Object getValue();
    int prepareType();
}
