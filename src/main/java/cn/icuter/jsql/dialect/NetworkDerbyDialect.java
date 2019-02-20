package cn.icuter.jsql.dialect;

/**
 * @author edward
 * @since 2018-08-30
 */
public class NetworkDerbyDialect extends EmbeddedDerbyDialect {
    @Override
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.ClientDriver";
    }

    @Override
    public String getDialectName() {
        return "derby://";
    }
}
