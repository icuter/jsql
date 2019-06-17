package cn.icuter.jsql.dialect;

/**
 * @author edward
 * @since 2018-08-30
 */
public class SQLServerDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getDialectName() {
        return "sqlserver";
    }

    @Override
    public String getQuoteString() {
        return "\"";
    }
}
