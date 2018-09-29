package cn.icuter.jsql;

import cn.icuter.jsql.datasource.JSQLDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author edward
 * @since 2018-09-20
 */
public class BaseDataSourceTest {

    protected static JSQLDataSource dataSource;

    static {
        Properties properties = new Properties();
        String icuterHome = System.getenv("ICUTER_HOME"); // only for test
        try (InputStream in = new FileInputStream(new File(icuterHome, "conf/jdbc.properties"))) {
            properties.load(in);
            dataSource = new JSQLDataSource(properties);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
