package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-21
 */
public class DataSourceException extends JSQLRuntimeException {
    public DataSourceException(String message) {
        super(message);
    }
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
    public DataSourceException(Throwable cause) {
        super(cause);
    }
}
