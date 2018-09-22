package cn.icuter.jsql.exception;

/**
 * @author edward
 * @since 2018-09-22
 */
public class TransactionRollbackExcetpion extends JSQLRuntimeException {
    public TransactionRollbackExcetpion(String message) {
        super(message);
    }
    public TransactionRollbackExcetpion(String message, Throwable cause) {
        super(message, cause);
    }
    public TransactionRollbackExcetpion(Throwable cause) {
        super(cause);
    }
}
