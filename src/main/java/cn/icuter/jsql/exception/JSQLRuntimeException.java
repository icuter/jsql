package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class JSQLRuntimeException extends RuntimeException {
    public JSQLRuntimeException(String message) {
        super(message);
    }
    public JSQLRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
    public JSQLRuntimeException(Throwable cause) {
        super(cause);
    }
}
