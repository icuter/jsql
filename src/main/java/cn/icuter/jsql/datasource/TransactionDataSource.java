package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.executor.TransactionExecutor;
import cn.icuter.jsql.transaction.Transaction;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TransactionDataSource extends AbstractBuilderDataSource implements Transaction, Closeable {
    private IgnoreCloseableTransactionExecutor executor;
    private JSQLDataSource dataSource;

    TransactionDataSource(JSQLDataSource dataSource) {
        this.dataSource = dataSource;
        this.executor = new IgnoreCloseableTransactionExecutor(dataSource.getTransactionExecutor());
    }

    public JSQLDataSource getJSQLDataSource() {
        return dataSource;
    }
    @Override
    public JdbcExecutor provideExecutor() {
        return executor;
    }

    @Override
    public Dialect provideDialect() {
        return dataSource.provideDialect();
    }

    @Override
    public boolean wasCommitted() {
        return executor.transactionExecutor.wasCommitted();
    }

    @Override
    public boolean wasRolledBack() {
        return executor.transactionExecutor.wasRolledBack();
    }

    @Override
    public State getState() {
        return executor.transactionExecutor.getState();
    }

    @Override
    public void commit() throws JSQLException {
        executor.transactionExecutor.commit();
    }

    @Override
    public void rollback() throws JSQLException {
        executor.transactionExecutor.rollback();
    }

    @Override
    public void addSavepoint(String name) throws JSQLException {
        executor.transactionExecutor.addSavepoint(name);
    }

    @Override
    public void rollback(String savePointName) throws JSQLException {
        executor.transactionExecutor.rollback(savePointName);
    }

    @Override
    public void releaseSavepoint(String savePointName) throws JSQLException {
        executor.transactionExecutor.releaseSavepoint(savePointName);
    }

    @Override
    public void releaseAllSavepoints() throws JSQLException {
        executor.transactionExecutor.releaseAllSavepoints();
    }

    @Override
    public void close() throws IOException {
        executor.transactionExecutor.close();
    }

    static class IgnoreCloseableTransactionExecutor implements JdbcExecutor {
        final TransactionExecutor transactionExecutor;
        IgnoreCloseableTransactionExecutor(TransactionExecutor transactionExecutor) {
            this.transactionExecutor = transactionExecutor;
        }
        @Override
        public int execUpdate(Builder builder) throws JSQLException {
            return transactionExecutor.execUpdate(builder);
        }
        @Override
        public <T> List<T> execQuery(Builder builder, Class<T> clazz) throws JSQLException {
            return transactionExecutor.execQuery(builder, clazz);
        }
        @Override
        public List<Map<String, Object>> execQuery(Builder builder) throws JSQLException {
            return transactionExecutor.execQuery(builder);
        }
        @Override
        public void execBatch(List<Builder> builders) throws JSQLException {
            transactionExecutor.execBatch(builders);
        }
        @Override
        public void close() {
            // noop
        }
    }
}
