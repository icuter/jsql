package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.builder.SQLStringBuilder;
import cn.icuter.jsql.condition.Cond;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author edward
 * @since 2018-08-30
 */
public abstract class Dialects {

    public static final Dialect MYSQL = new MySQLDialect();
    public static final Dialect MARIADB = new MariaDBDialect();
    public static final Dialect SQLSERVER = new SQLServerDialect();
    public static final Dialect SQLSERVER2012_PLUS = new SQLServer2012PlusDialect();
    public static final Dialect ORACLE = new OracleDialect();
    public static final Dialect H2 = new H2Dialect();
    public static final Dialect EMBEDDED_DERBY = new EmbeddedDerbyDialect();
    public static final Dialect NETWORK_DERBY = new NetworkDerbyDialect();
    public static final Dialect POSTGRESQL = new PostgreSQLDialect();
    public static final Dialect CUBRID = new CubridDialect();
    public static final Dialect DB2 = new DB2Dialect();
    public static final Dialect SQLITE = new SQLiteDialect();
    public static final Dialect UNKNOWN = new UnknownDialect();

    public static final Set<Dialect> SUPPORTED_DIALECT = new LinkedHashSet<>();
    static {
        SUPPORTED_DIALECT.add(MYSQL);
        SUPPORTED_DIALECT.add(MARIADB);
        SUPPORTED_DIALECT.add(SQLSERVER2012_PLUS); // prior to SQLSERVER dialect for intelligent guess
        SUPPORTED_DIALECT.add(ORACLE);
        SUPPORTED_DIALECT.add(H2);
        SUPPORTED_DIALECT.add(NETWORK_DERBY);
        SUPPORTED_DIALECT.add(EMBEDDED_DERBY);
        SUPPORTED_DIALECT.add(POSTGRESQL);
        SUPPORTED_DIALECT.add(CUBRID);
        SUPPORTED_DIALECT.add(DB2);
        SUPPORTED_DIALECT.add(SQLITE);
        SUPPORTED_DIALECT.add(SQLSERVER);
        SUPPORTED_DIALECT.add(UNKNOWN);
    }

    public static Dialect parseUrl(String url) {
        Objects.requireNonNull(url, "Url must not be null");
        final String lowerCaseUrl = url.toLowerCase();
        return SUPPORTED_DIALECT.stream()
                .filter(dialect -> lowerCaseUrl.startsWith("jdbc:" + dialect.getDialectName().toLowerCase()))
                .findFirst().orElse(Dialects.UNKNOWN);
    }

    public static Dialect parseName(String nameOrDialectClass) {
        Objects.requireNonNull(nameOrDialectClass, "Dialect Name Or Dialect Class must not be null");
        return SUPPORTED_DIALECT.stream()
                .filter(dialect -> nameOrDialectClass.equalsIgnoreCase(dialect.getDialectName())
                        || nameOrDialectClass.equalsIgnoreCase(dialect.getClass().getName()))
                .findFirst().orElse(Dialects.UNKNOWN);
    }

    static void injectWithLimitKey(BuilderContext builderContext) {
        boolean offsetExists = builderContext.getOffset() > 0;
        StringBuilder offsetLimitBuilder = new StringBuilder(offsetExists ? "limit ?,?" : "limit ?");
        if (offsetExists) {
            builderContext.addCondition(Cond.value(builderContext.getOffset()));
        }
        builderContext.addCondition(Cond.value(builderContext.getLimit()));

        SQLStringBuilder sqlStringBuilder = builderContext.getSqlStringBuilder();
        if (builderContext.getForUpdatePosition() > 0) {
            sqlStringBuilder.insert(builderContext.getForUpdatePosition(), offsetLimitBuilder.toString());
        } else {
            sqlStringBuilder.append(offsetLimitBuilder.toString());
        }
    }

    static void injectWithLimitOffsetKey(BuilderContext builderContext) {
        boolean offsetExists = builderContext.getOffset() > 0;
        StringBuilder offsetLimitBuilder = new StringBuilder(offsetExists ? "limit ? offset ?" : "limit ?");
        builderContext.addCondition(Cond.value(builderContext.getLimit()));
        if (offsetExists) {
            builderContext.addCondition(Cond.value(builderContext.getOffset()));
        }
        SQLStringBuilder sqlStringBuilder = builderContext.getSqlStringBuilder();
        if (builderContext.getForUpdatePosition() > 0) {
            sqlStringBuilder.insert(builderContext.getForUpdatePosition(), offsetLimitBuilder.toString());
        } else {
            sqlStringBuilder.append(offsetLimitBuilder.toString());
        }
    }
    static String getRowNumberAlias(BuilderContext builderContext) {
        return "rownumber_" + builderContext.getSqlLevel() + "_";
    }
}
