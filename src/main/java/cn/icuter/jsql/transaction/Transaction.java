package cn.icuter.jsql.transaction;

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

    void commit();
    void rollback();

    void addSavepoint(String name);
    void rollback(String savePointName);
    void releaseSavepoint(String savePointName);

    void setStateListener(StateListener listener);

    @FunctionalInterface
    interface StateListener {
        void fireEvent(Transaction transaction, State state);
    }
}
