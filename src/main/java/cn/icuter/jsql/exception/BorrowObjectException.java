package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class BorrowObjectException extends JSQLRuntimeException {
    public BorrowObjectException(String message) {
        super(message);
    }
    public BorrowObjectException(String message, Throwable cause) {
        super(message, cause);
    }
    public BorrowObjectException(Throwable cause) {
        super(cause);
    }
}
