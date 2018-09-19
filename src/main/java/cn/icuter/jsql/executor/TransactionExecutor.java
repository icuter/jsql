package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.transaction.DefaultTransaction;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author edward
 * @since 2018-09-13
 */
public class TransactionExecutor extends DefaultTransaction implements JdbcExecutor {

    private final JdbcExecutor jdbcExecutor;

    public TransactionExecutor(Connection connection) {
        super(connection);
        jdbcExecutor = new DefaultJdbcExecutor(connection);
    }

    @Override
    public int execUpdate(Builder builder) throws Exception {
        try {
            return jdbcExecutor.execUpdate(builder);
        } catch (Exception e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public <T> List<T> execQuery(Builder builder, Class<T> clazz) throws Exception {
        try {
            return jdbcExecutor.execQuery(builder, clazz);
        } catch (Exception e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public List<Map<String, Object>> execQuery(Builder builder) throws Exception {
        try {
            return jdbcExecutor.execQuery(builder);
        } catch (Exception e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public void execBatch(List<Builder> builders, BatchCompletedAction completedAction) throws Exception {
        try {
            jdbcExecutor.execBatch(builders, completedAction);
        } catch (Exception e) {
            setState(State.ERROR);
            throw e;
        }
    }
}
