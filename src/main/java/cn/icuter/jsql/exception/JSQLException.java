package cn.icuter.jsql.exception;

import java.sql.SQLException;

/**
 * @author edward
 * @since 2018-09-16
 */
public class JSQLException extends SQLException {
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
