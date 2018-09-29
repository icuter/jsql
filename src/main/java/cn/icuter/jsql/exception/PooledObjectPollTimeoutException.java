package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class PooledObjectPollTimeoutException extends JSQLException {
    public PooledObjectPollTimeoutException(String message) {
        super(message);
    }
    public PooledObjectPollTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
    public PooledObjectPollTimeoutException(Throwable cause) {
        super(cause);
    }
}
