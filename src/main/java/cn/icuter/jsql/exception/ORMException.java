package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class ORMException extends JSQLRuntimeException {
    public ORMException(String message) {
        super(message);
    }
    public ORMException(String message, Throwable cause) {
        super(message, cause);
    }
    public ORMException(Throwable cause) {
        super(cause);
    }
}
