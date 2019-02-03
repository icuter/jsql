package cn.icuter.jsql.builder;

import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.JdbcExecutor;

import java.util.List;
import java.util.Map;

/**
 * @author edward
 * @since 2019-01-11
 */
public interface ExecutableBuilder {

    <E> List<E> execQuery(JdbcExecutor executor, Class<E> clazz) throws JSQLException;
    <E> List<E> execQuery(Class<E> clazz) throws JSQLException;

    List<Map<String, Object>> execQuery(JdbcExecutor executor) throws JSQLException;
    List<Map<String, Object>> execQuery() throws JSQLException;

    int execUpdate(JdbcExecutor executor) throws JSQLException;
    int execUpdate() throws JSQLException;

}
