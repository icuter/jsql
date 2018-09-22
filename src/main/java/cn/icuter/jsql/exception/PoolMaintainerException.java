package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class PoolMaintainerException extends JSQLRuntimeException {
    public PoolMaintainerException(String message) {
        super(message);
    }
    public PoolMaintainerException(String message, Throwable cause) {
        super(message, cause);
    }
    public PoolMaintainerException(Throwable cause) {
        super(cause);
    }
}
