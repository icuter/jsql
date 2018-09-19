package cn.icuter.jsql.transaction;

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
            // TODO log
            e.printStackTrace();
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
    public void commit() {
        try {
            connection.commit();
            setState(State.COMMIT);
        } catch (SQLException e) {
            // TODO log
            setState(State.COMMIT_ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void rollback() {
        try {
            connection.rollback();
            setState(State.ROLLBACK);
        } catch (SQLException e) {
            // TODO log
            setState(State.ROLLBACK_ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void addSavepoint(String savepointName) {
        try {
            // check name whether unique
            if (savepointMap.containsKey(savepointName)) {
                throw new IllegalArgumentException(savepointName + " has been defined!");
            }
            savepointMap.put(savepointName, connection.setSavepoint(savepointName));
        } catch (SQLException e) {
            // TODO log
            setState(State.ADD_SAVEPOINT_ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void rollback(String savepointName) {
        try {
            connection.rollback(savepointMap.get(savepointName));
            setState(State.ROLLBACK_SAVEPOINT);
        } catch (SQLException e) {
            // TODO log
            setState(State.ROLLBACK_SAVEPOINT_ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void releaseSavepoint(String savepointName) {
        try {
            connection.releaseSavepoint(savepointMap.get(savepointName));
        } catch (SQLException e) {
            // TODO log
            setState(State.RELEASE_SAVEPOINT_ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    protected void setState(State state) {
        this.state = state;
        stateListener.fireEvent(this, this.state);
    }
}
