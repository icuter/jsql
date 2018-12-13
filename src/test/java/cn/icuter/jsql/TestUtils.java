package cn.icuter.jsql;

import cn.icuter.jsql.datasource.JSQLDataSource;

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

    public static JSQLDataSource getDataSource(String propName) {
        Properties properties = new Properties();
        String icuterHome = System.getenv("ICUTER_HOME"); // only for test
        InputStream in = null;
        try {
            in = new FileInputStream(new File(icuterHome, "conf/" + propName + ".properties"));
            properties.load(in);
            return new JSQLDataSource(properties);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
