package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class PooledObjectCreationException extends JSQLException {
    public PooledObjectCreationException(String message) {
        super(message);
    }
    public PooledObjectCreationException(String message, Throwable cause) {
        super(message, cause);
    }
    public PooledObjectCreationException(Throwable cause) {
        super(cause);
    }
}
