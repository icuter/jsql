package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class PoolCloseException extends JSQLRuntimeException {
    public PoolCloseException(String message) {
        super(message);
    }
    public PoolCloseException(String message, Throwable cause) {
        super(message, cause);
    }
    public PoolCloseException(Throwable cause) {
        super(cause);
    }
}
