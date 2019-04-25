package cn.icuter.jsql;

import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.executor.JdbcExecutorTest;
import cn.icuter.jsql.executor.ORMTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author edward
 * @since 2019-02-12
 */
public abstract class TestUtils {
    public static String DB_TYPE_KEY = "dbType";
    public static String DEFAULT_DB_TYPE = "derby";

    public static JSQLDataSource getDataSource() {
        Properties properties = new Properties();
        String dbType = System.getProperty(TestUtils.DB_TYPE_KEY, DEFAULT_DB_TYPE);
        String icuterHome = System.getenv("ICUTER_HOME"); // only for test
        File jdbcPropFile = new File(icuterHome, String.format("conf/%s.properties", dbType.toLowerCase()));
        try (InputStream in = new FileInputStream(jdbcPropFile)) {
            properties.load(in);
            return new JSQLDataSource(properties);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getCreateJdbcTableSql() {
        String dbType = System.getProperty(TestUtils.DB_TYPE_KEY, DEFAULT_DB_TYPE);
        if (dbType == null || dbType.isEmpty()) {
            throw new IllegalArgumentException("dbType must not be null or empty");
        }
        if ("derby".equalsIgnoreCase(dbType)) {
            return "CREATE TABLE " + JdbcExecutorTest.TABLE_NAME + "\n" +
                    "(\n" +
                    "  test_id VARCHAR(60) NOT NULL,\n" +
                    "  t_col_1 VARCHAR(60),\n" +
                    "  t_col_2 VARCHAR(60),\n" +
                    "  order_num INTEGER,\n" +
                    "  PRIMARY KEY (test_id))";
        }
        return "CREATE TABLE " + JdbcExecutorTest.TABLE_NAME + "\n" +
                "(\n" +
                "  test_id VARCHAR(60) NOT NULL,\n" +
                "  t_col_1 VARCHAR(60) NULL,\n" +
                "  t_col_2 VARCHAR(60) NULL,\n" +
                "  order_num INTEGER NULL,\n" +
                "  PRIMARY KEY (test_id))";
    }

    public static String getCreateOrmTableSql() {
        String dbType = System.getProperty(TestUtils.DB_TYPE_KEY, DEFAULT_DB_TYPE);
        if (dbType == null || dbType.isEmpty()) {
            throw new IllegalArgumentException("dbType must not be null or empty");
        }
        if ("mariadb".equalsIgnoreCase(dbType) || "mysql".equalsIgnoreCase(dbType)) {
            return "CREATE TABLE " + ORMTest.TABLE_NAME + "\n" +
                    "(\n" +
                    "  orm_id VARCHAR(60) NOT NULL,\n" +
                    "  f_blob BLOB NULL,\n" +
                    "  f_string VARCHAR(60) NULL,\n" +
                    "  f_int INTEGER NULL,\n" +
                    "  f_integer INTEGER NULL,\n" +
                    "  f_double decimal(10,3) NULL,\n" +
                    "  f_decimal decimal(10,3) NULL,\n" +
                    "  f_clob TEXT NULL,\n" + // mysql/mariadb
                    "  PRIMARY KEY (orm_id))";
        } else if ("postgresql".equalsIgnoreCase(dbType)) {
            return "CREATE TABLE " + ORMTest.TABLE_NAME + "\n" +
                    "(\n" +
                    "  orm_id VARCHAR(60) NOT NULL,\n" +
                    "  f_blob bytea NULL,\n" + // postgreSQL
                    "  f_string VARCHAR(60) NULL,\n" +
                    "  f_int INTEGER NULL,\n" +
                    "  f_integer INTEGER NULL,\n" +
                    "  f_double decimal(10,3) NULL,\n" +
                    "  f_decimal decimal(10,3) NULL,\n" +
                    "  f_clob TEXT NULL,\n" +
                    "  PRIMARY KEY (orm_id))";
        } else if ("derby".equalsIgnoreCase(dbType)) {
            return "CREATE TABLE " + ORMTest.TABLE_NAME + "\n" +
                    "(\n" +
                    "  orm_id VARCHAR(60) NOT NULL,\n" +
                    "  f_blob BLOB,\n" + // postgreSQL
                    "  f_string VARCHAR(60),\n" +
                    "  f_int INTEGER,\n" +
                    "  f_integer INTEGER,\n" +
                    "  f_double decimal(10,3),\n" +
                    "  f_decimal decimal(10,3),\n" +
                    "  f_clob CLOB,\n" +
                    "  PRIMARY KEY (orm_id))";
        } else if ("sqlserver".equalsIgnoreCase(dbType)) {
            return "CREATE TABLE " + ORMTest.TABLE_NAME + "\n" +
                    "(\n" +
                    "  orm_id VARCHAR(60) NOT NULL,\n" +
                    "  f_blob VARBINARY(1000),\n" +
                    "  f_string VARCHAR(60),\n" +
                    "  f_int INTEGER,\n" +
                    "  f_integer INTEGER,\n" +
                    "  f_double decimal(10,3),\n" +
                    "  f_decimal decimal(10,3),\n" +
                    "  f_clob text,\n" +
                    "  PRIMARY KEY (orm_id))";
        }
        return "CREATE TABLE " + ORMTest.TABLE_NAME + "\n" +
                "(\n" +
                "  orm_id VARCHAR(60) NOT NULL,\n" +
                "  f_blob BLOB NULL,\n" +
                "  f_string VARCHAR(60) NULL,\n" +
                "  f_int INTEGER NULL,\n" +
                "  f_integer INTEGER NULL,\n" +
                "  f_double decimal(10,3) NULL,\n" +
                "  f_decimal decimal(10,3) NULL,\n" +
                "  f_clob CLOB NULL,\n" +
                "  PRIMARY KEY (orm_id))";
    }
}
