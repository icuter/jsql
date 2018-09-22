package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-22
 */
public class TransactionCommitException extends JSQLRuntimeException {
    public TransactionCommitException(String message) {
        super(message);
    }
    public TransactionCommitException(String message, Throwable cause) {
        super(message, cause);
    }
    public TransactionCommitException(Throwable cause) {
        super(cause);
    }
}
