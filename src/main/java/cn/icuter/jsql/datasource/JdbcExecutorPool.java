package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.exception.BorrowObjectException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolCloseException;
import cn.icuter.jsql.exception.ReturnObjectException;
import cn.icuter.jsql.executor.DefaultJdbcExecutor;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.executor.TransactionExecutor;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.pool.ObjectPool;
import cn.icuter.jsql.transaction.Transaction;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author edward
 * @since 2018-09-13
 */
public class JdbcExecutorPool {
    private static final JSQLLogger LOGGER = Logs.getLogger(JdbcExecutorPool.class);
    private final ObjectPool<Connection> pool;

    JdbcExecutorPool(ObjectPool<Connection> pool) {
        this.pool = pool;
    }

    public JdbcExecutor getExecutor() {
        try {
            return new ConnectionJdbcExecutor(pool.borrowObject());
        } catch (JSQLException e) {
            throw new BorrowObjectException("getting JdbcExecutor error", e);
        }
    }

    public TransactionExecutor getTransactionExecutor() {
        try {
            Connection connection = pool.borrowObject();
            connection.setAutoCommit(false);
            return new ConnectionTransactionExecutor(connection);
        } catch (SQLException e) {
            LOGGER.error("getting TransactionExecutor error", e);
            throw new BorrowObjectException("getting TransactionExecutor error", e);
        }
    }

    public void returnExecutor(JdbcExecutor executor) {
        try {
            if (executor instanceof ConnectionExecutor) {
                ConnectionExecutor connExecutor = (ConnectionExecutor) executor;
                if (connExecutor.isReleased()) {
                    throw new ReturnObjectException("executor has been returned");
                }
                if (connExecutor instanceof ConnectionTransactionExecutor) {
                    ConnectionTransactionExecutor txExecutor = ((ConnectionTransactionExecutor) connExecutor);
                    if (txExecutor.getState() == Transaction.State.ROLLBACK_SAVEPOINT
                            || (!txExecutor.wasCommitted() && !txExecutor.wasRolledBack())) {
                        txExecutor.commit();
                    }
                }
                if (connExecutor.isTransaction()) {
                    // if transaction did not commit, setAutoCommit(true) will commit automatically
                    connExecutor.getConnection().setAutoCommit(true);
                }
                pool.returnObject(connExecutor.getConnection());
                connExecutor.release(); // in case reused after transaction executor returned
            }
        } catch (SQLException e) {
            LOGGER.error("returning Executor error", e);
            throw new ReturnObjectException("returning Executor error", e);
        }
    }

    public void close() {
        try {
            pool.close();
        } catch (JSQLException e) {
            LOGGER.error("closing ExecutorPool error", e);
            throw new PoolCloseException("closing ExecutorPool error", e);
        }
    }

    public String debugInfo() {
        return pool.debugInfo();
    }

    class ConnectionJdbcExecutor extends DefaultJdbcExecutor implements ConnectionExecutor {
        boolean release;
        ConnectionJdbcExecutor(Connection connection) {
            super(connection);
        }
        @Override
        public Connection getConnection() {
            return connection;
        }
        @Override
        public void release() {
            release = true;
        }
        @Override
        public boolean isReleased() {
            return release;
        }
        @Override
        public boolean isTransaction() {
            return false;
        }
        @Override
        public JdbcExecutor getExecutor() {
            return this;
        }
        @Override
        public void close() throws IOException {
            returnExecutor(this);
        }
        @Override
        public List<Map<String, Object>> execQuery(Builder builder) throws JSQLException {
            return super.execQuery(builder);
        }
        @Override
        public <T> List<T> execQuery(Builder builder, Class<T> clazz) throws JSQLException {
            return super.execQuery(builder, clazz);
        }
        @Override
        public int execUpdate(Builder builder) throws JSQLException {
            return super.execUpdate(builder);
        }
        @Override
        public void execBatch(List<Builder> builders) throws JSQLException {
            super.execBatch(builders);
        }
    }

    class ConnectionTransactionExecutor extends TransactionExecutor implements ConnectionExecutor {
        boolean release;
        ConnectionTransactionExecutor(Connection connection) {
            super(connection);
        }
        @Override
        public Connection getConnection() {
            return connection;
        }
        @Override
        public void release() {
            release = true;
        }
        @Override
        public boolean isReleased() {
            return release;
        }
        @Override
        public boolean isTransaction() {
            return true;
        }
        @Override
        public JdbcExecutor getExecutor() {
            return this;
        }
        @Override
        public void close() throws IOException {
            returnExecutor(this);
        }
        @Override
        public List<Map<String, Object>> execQuery(Builder builder) throws JSQLException {
            return super.execQuery(builder);
        }
        @Override
        public <T> List<T> execQuery(Builder builder, Class<T> clazz) throws JSQLException {
            return super.execQuery(builder, clazz);
        }
        @Override
        public int execUpdate(Builder builder) throws JSQLException {
            return super.execUpdate(builder);
        }
        @Override
        public void execBatch(List<Builder> builders) throws JSQLException {
            super.execBatch(builders);
        }
    }

    interface ConnectionExecutor {
        Connection getConnection();
        void release();
        boolean isReleased();
        boolean isTransaction();
        JdbcExecutor getExecutor();
    }
}
