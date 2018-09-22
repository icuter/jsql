package cn.icuter.jsql.transaction;

import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.TransactionException;

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
    private final Map<String, Savepoint> savepointMap;
    private final Connection connection;
    private StateListener stateListener = (transaction, state) -> { };
    private State state;

    public DefaultTransaction(Connection connection) {
        checkConnection(connection);
        this.connection = connection;
        savepointMap = new LinkedHashMap<>();
    }

    private void checkConnection(Connection connection) {
        try {
            if (connection.getAutoCommit()) {
                throw new IllegalStateException("Illegal for auto commit Connection!");
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("unavailable Connection", e);
        }
    }

    @Override
    public boolean wasCommitted() {
        return state == State.COMMIT;
    }

    @Override
    public boolean wasRolledBack() {
        return state == State.ROLLBACK;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void commit() throws JSQLException {
        try {
            connection.commit();
            setState(State.COMMIT);
        } catch (SQLException e) {
            setState(State.COMMIT_ERROR);
            throw new TransactionException("commit transaction error", e);
        }
    }

    @Override
    public void rollback() throws JSQLException {
        try {
            connection.rollback();
            setState(State.ROLLBACK);
        } catch (SQLException e) {
            setState(State.ROLLBACK_ERROR);
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
            throw new TransactionException("adding savepoint error for name: " + savepointName, e);
        }
    }

    @Override
    public void rollback(String savepointName) throws JSQLException {
        try {
            connection.rollback(savepointMap.get(savepointName));
            setState(State.ROLLBACK_SAVEPOINT);
        } catch (SQLException e) {
            setState(State.ROLLBACK_SAVEPOINT_ERROR);
            throw new TransactionException("rolling back savepoint error for name: " + savepointName, e);
        }
    }

    @Override
    public void releaseSavepoint(String savepointName) throws JSQLException {
        try {
            connection.releaseSavepoint(savepointMap.get(savepointName));
        } catch (SQLException e) {
            setState(State.RELEASE_SAVEPOINT_ERROR);
            throw new TransactionException("releasing savepoint error for name: " + savepointName, e);
        }
    }

    @Override
    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    protected void setState(State state) throws JSQLException {
        this.state = state;
        stateListener.fireEvent(this, this.state);
    }
}
