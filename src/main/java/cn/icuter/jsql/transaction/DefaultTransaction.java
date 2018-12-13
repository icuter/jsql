package cn.icuter.jsql.transaction;

import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.TransactionException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author edward
 * @since 2018-09-16
 */
public class DefaultTransaction implements Transaction {
    private static final JSQLLogger LOGGER = Logs.getLogger(DefaultTransaction.class);
    private final Map<String, Savepoint> savepointMap;
    protected final Connection connection;
    private State state;
    private boolean committed;
    private boolean rolledBack;

    public DefaultTransaction(Connection connection) {
        checkConnection(connection);
        this.connection = connection;
        savepointMap = new LinkedHashMap<String, Savepoint>();
    }

    private void checkConnection(Connection connection) {
        try {
            if (connection.getAutoCommit()) {
                throw new IllegalStateException("invalid state for auto commit Connection!");
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("unavailable Connection", e);
        }
    }

    @Override
    public boolean wasCommitted() {
        return committed;
    }

    @Override
    public boolean wasRolledBack() {
        return rolledBack;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void commit() throws JSQLException {
        try {
            connection.commit();
            committed = true;
            setState(State.COMMIT);
        } catch (SQLException e) {
            setState(State.COMMIT_ERROR);
            LOGGER.error("commit transaction error", e);
            throw new TransactionException("commit transaction error", e);
        }
    }

    @Override
    public void rollback() throws JSQLException {
        try {
            connection.rollback();
            rolledBack = true;
            setState(State.ROLLBACK);
        } catch (SQLException e) {
            setState(State.ROLLBACK_ERROR);
            LOGGER.error("rolling back transaction error", e);
            throw new TransactionException("rolling back transaction error", e);
        }
    }

    @Override
    public void addSavepoint(String savepointName) throws JSQLException {
        try {
            // check name whether unique
            if (savepointMap.containsKey(savepointName)) {
                throw new IllegalArgumentException(savepointName + " has been defined!");
            }
            savepointMap.put(savepointName, connection.setSavepoint(savepointName));
        } catch (SQLException e) {
            setState(State.ADD_SAVEPOINT_ERROR);
            LOGGER.error("adding savepoint error for name: " + savepointName, e);
            throw new TransactionException("adding savepoint error for name: " + savepointName, e);
        }
    }

    @Override
    public void rollback(String savepointName) throws JSQLException {
        try {
            Savepoint savepoint = savepointMap.get(savepointName);
            if (savepoint != null) {
                connection.rollback(savepoint);
                setState(State.ROLLBACK_SAVEPOINT);
            }
        } catch (SQLException e) {
            setState(State.ROLLBACK_SAVEPOINT_ERROR);
            LOGGER.error("rolling back savepoint error for name: " + savepointName, e);
            throw new TransactionException("rolling back savepoint error for name: " + savepointName, e);
        }
    }

    @Override
    public void releaseSavepoint(String savepointName) throws JSQLException {
        try {
            connection.releaseSavepoint(savepointMap.get(savepointName));
        } catch (SQLException e) {
            setState(State.RELEASE_SAVEPOINT_ERROR);
            LOGGER.error("releasing savepoint error for name: " + savepointName, e);
            throw new TransactionException("releasing savepoint error for name: " + savepointName, e);
        }
    }

    protected void setState(State state) throws JSQLException {
        this.state = state;
    }

    @Override
    public void releaseAllSavepoints() throws JSQLException {
        for (Map.Entry<String, Savepoint> entry : savepointMap.entrySet()) {
            releaseSavepoint(entry.getKey());
        }
    }
}
