package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-16
 */
public class ExecutionException extends JSQLException {
    public ExecutionException(String message) {
        super(message);
    }
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    public ExecutionException(Throwable cause) {
        super(cause);
    }
}
