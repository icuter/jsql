package cn.icuter.jsql.transaction;

import cn.icuter.jsql.exception.JSQLException;

/**
 * @author edward
 * @since 2018-09-15
 */
public interface Transaction {

    enum State {
        ROLLBACK,
        ROLLBACK_SAVEPOINT,
        COMMIT,
        ERROR,
        RELEASE_SAVEPOINT_ERROR,
        ADD_SAVEPOINT_ERROR,
        COMMIT_ERROR,
        ROLLBACK_ERROR,
        ROLLBACK_SAVEPOINT_ERROR,
    }
    boolean wasCommitted();
    boolean wasRolledBack();
    State getState();

    void commit() throws JSQLException;
    void rollback() throws JSQLException;

    void addSavepoint(String name) throws JSQLException;
    void rollback(String savePointName) throws JSQLException;
    void releaseSavepoint(String savePointName) throws JSQLException;

    void setStateListener(StateListener listener);

    @FunctionalInterface
    interface StateListener {
        void fireEvent(Transaction transaction, State state) throws JSQLException;
    }
}
