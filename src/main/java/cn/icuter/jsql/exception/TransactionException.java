package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class TransactionException extends JSQLException {
    public TransactionException(String message) {
        super(message);
    }
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
    public TransactionException(Throwable cause) {
        super(cause);
    }
}
