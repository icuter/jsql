package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-16
 */
public class JSQLException extends Exception {
    public JSQLException(String message) {
        super(message);
    }
    public JSQLException(String message, Throwable cause) {
        super(message, cause);
    }
    public JSQLException(Throwable cause) {
        super(cause);
    }
}
