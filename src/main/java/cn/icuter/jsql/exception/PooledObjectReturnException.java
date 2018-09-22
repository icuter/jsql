package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class PooledObjectReturnException extends JSQLException {
    public PooledObjectReturnException(String message) {
        super(message);
    }
    public PooledObjectReturnException(String message, Throwable cause) {
        super(message, cause);
    }
    public PooledObjectReturnException(Throwable cause) {
        super(cause);
    }
}
