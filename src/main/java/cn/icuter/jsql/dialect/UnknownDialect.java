package cn.icuter.jsql.dialect;

/**
 * @author edward
 * @since 2018-08-30
 */
public class UnknownDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        throw new UnsupportedOperationException("no driver for unknown dialect!");
    }

    @Override
    public String getDialectName() {
        return "unknown";
    }
}
