package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.TransactionCommitException;
import cn.icuter.jsql.exception.TransactionRollbackExcetpion;
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
    public int execUpdate(Builder builder) throws JSQLException {
        try {
            return jdbcExecutor.execUpdate(builder);
        } catch (JSQLException e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public <T> List<T> execQuery(Builder builder, Class<T> clazz) throws JSQLException {
        try {
            return jdbcExecutor.execQuery(builder, clazz);
        } catch (JSQLException e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public List<Map<String, Object>> execQuery(Builder builder) throws JSQLException {
        try {
            return jdbcExecutor.execQuery(builder);
        } catch (JSQLException e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public void execBatch(List<Builder> builders, BatchCompletedAction completedAction) throws JSQLException {
        try {
            jdbcExecutor.execBatch(builders, completedAction);
        } catch (JSQLException e) {
            setState(State.ERROR);
            throw e;
        }
    }

    @Override
    public void commit() {
        try {
            super.commit();
        } catch (JSQLException e) {
            throw new TransactionCommitException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            super.rollback();
        } catch (JSQLException e) {
            throw new TransactionRollbackExcetpion(e);
        }
    }

    @Override
    public void rollback(String savepointName) {
        try {
            super.rollback(savepointName);
        } catch (JSQLException e) {
            throw new TransactionRollbackExcetpion(e);
        }
    }
}
