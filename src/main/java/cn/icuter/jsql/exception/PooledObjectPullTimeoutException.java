package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class PooledObjectPullTimeoutException extends JSQLException {
    public PooledObjectPullTimeoutException(String message) {
        super(message);
    }
    public PooledObjectPullTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
    public PooledObjectPullTimeoutException(Throwable cause) {
        super(cause);
    }
}
