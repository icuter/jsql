package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.executor.DefaultJdbcExecutor;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.pool.PooledObject;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author edward
 * @since 2018-08-26
 */
public class PooledJdbcExecutor extends PooledObject<Connection> implements JdbcExecutor {

    private JdbcExecutor jdbcExecutor;

    public PooledJdbcExecutor(Connection connection) {
        this(connection, new DefaultJdbcExecutor(connection));
    }

    public PooledJdbcExecutor(Connection connection, JdbcExecutor jdbcExecutor) {
        super(connection);
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public int execUpdate(Builder builder) throws Exception {
        checkAvailable();
        return jdbcExecutor.execUpdate(builder);
    }

    @Override
    public <T> List<T> execQuery(Builder builder, Class<T> clazz) throws Exception {
        checkAvailable();
        return jdbcExecutor.execQuery(builder, clazz);
    }

    @Override
    public List<Map<String, Object>> execQuery(Builder builder) throws Exception {
        checkAvailable();
        return jdbcExecutor.execQuery(builder);
    }

    @Override
    public void execBatch(List<Builder> builders, BatchCompletedAction batchCompletedAction) throws Exception {
        checkAvailable();
        jdbcExecutor.execBatch(builders, batchCompletedAction);
    }

    private void checkAvailable() {
        if (!isBorrowed()) {
            throw new IllegalStateException("Executor has been returned, please borrow another one!");
        }
    }
}
