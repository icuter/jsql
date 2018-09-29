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
    void end() throws JSQLException;

    void addSavepoint(String name) throws JSQLException;
    void rollback(String savePointName) throws JSQLException;

    /**
     * <pre>
     * Release a savepoint by name in transaction that is before commit/rollback
     *
     * <em>Note</em>
     * Savepoint should be released after transaction end with commit/rollback operation
     * Refer to mysql docs <a href="https://dev.mysql.com/doc/refman/5.6/en/savepoint.html">RELEASE SAVEPOINT</a>
     * <em>&gt; All savepoints of the current transaction are deleted if you execute a COMMIT, or a ROLLBACK that does not name a savepoint. </em>
     *
     * Refer to oracle docs <a href="https://docs.oracle.com/cd/B19306_01/appdev.102/b14261/savepoint_statement.htm">SAVEPOINT Statement</a>
     * <em>&gt; A simple rollback or commit erases all savepoints.</em>
     * </pre>
     * @param savePointName savepoint's name
     */
    void releaseSavepoint(String savePointName) throws JSQLException;

    /**
     * <pre>
     * Release all savepoint in transaction that is before commit/rollback
     *
     * <em>Note</em>
     * Savepoint should be released after transaction end with commit/rollback operation
     * Refer to mysql docs <a href="https://dev.mysql.com/doc/refman/5.6/en/savepoint.html">RELEASE SAVEPOINT</a>
     * <em>&gt; All savepoints of the current transaction are deleted if you execute a COMMIT, or a ROLLBACK that does not name a savepoint. </em>
     *
     * Refer to oracle docs <a href="https://docs.oracle.com/cd/B19306_01/appdev.102/b14261/savepoint_statement.htm">SAVEPOINT Statement</a>
     * <em>&gt; A simple rollback or commit erases all savepoints.</em>
     * </pre>
     */
    void releaseAllSavepoints() throws JSQLException;

    void setStateListener(StateListener listener);

    @FunctionalInterface
    interface StateListener {
        void fireEvent(Transaction transaction, State state) throws JSQLException;
    }
}
