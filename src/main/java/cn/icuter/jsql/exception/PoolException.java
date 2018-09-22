package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-20
 */
public class PoolException extends JSQLException {
    public PoolException(String message) {
        super(message);
    }
    public PoolException(String message, Throwable cause) {
        super(message, cause);
    }
    public PoolException(Throwable cause) {
        super(cause);
    }
}
