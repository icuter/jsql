package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.exception.JSQLException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * For {@link Builder} execution
 *
 * @author edward
 * @since 2018-08-07
 */
public interface JdbcExecutor extends Closeable {
    int execUpdate(Builder builder) throws JSQLException;

    <T> List<T> execQuery(Builder builder, Class<T> clazz) throws JSQLException;

    List<Map<String, Object>> execQuery(Builder builder) throws JSQLException;

    void execBatch(List<Builder> builders) throws JSQLException;

    default void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    interface BatchCompletedAction {
        void doAction(BatchEvent event) throws JSQLException;
    }
}
