package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;

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
    int execUpdate(Builder builder) throws Exception;

    <T> List<T> execQuery(Builder builder, Class<T> clazz) throws Exception;

    List<Map<String, Object>> execQuery(Builder builder) throws Exception;

    void execBatch(List<Builder> builders, BatchCompletedAction completedAction) throws Exception;

    default void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    interface BatchCompletedAction {
        void doAction(BatchEvent event) throws Exception;
    }
}
