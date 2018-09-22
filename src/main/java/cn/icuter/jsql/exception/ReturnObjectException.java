package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class ReturnObjectException extends JSQLRuntimeException {
    public ReturnObjectException(String message) {
        super(message);
    }
    public ReturnObjectException(String message, Throwable cause) {
        super(message, cause);
    }
    public ReturnObjectException(Throwable cause) {
        super(cause);
    }
}
