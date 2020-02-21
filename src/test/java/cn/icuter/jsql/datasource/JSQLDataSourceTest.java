package cn.icuter.jsql.datasource;

import cn.icuter.jsql.ExceptionOperation;
import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.dialect.Dialects;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JSQLDataSourceTest {
    private String jdbcUrl = "jdbc:mariadb://192.168.200.96:3307/testdb?serverTimezone=GMT%2B8";

    @Test
    public void testNewInstance() {
        JSQLDataSource dataSource = new JSQLDataSource(
                "jdbc:h2:tcp://192.168.200.96:1522/testdb",
                "icuter",
                "pass");
        Assert.assertEquals(Dialects.H2.getDriverClassName(), dataSource.getDriverClassName());
        Assert.assertEquals(Dialects.H2, dataSource.getDialect());
        Assert.assertEquals(5, dataSource.getLoginTimeout());
        Properties driverProps = dataSource.getDriverProperties();
        Assert.assertEquals("icuter", driverProps.getProperty("user"));
        Assert.assertEquals("pass", driverProps.getProperty("password"));
    }

    @Test
    public void testNewInstanceByProperties() {
        Properties props = new Properties();
        props.setProperty("url", jdbcUrl);
        props.setProperty("loginTimeout", "10");
        props.setProperty("username", "jsql"); // will be overridden by driver.user
        props.setProperty("driver.user", "icuter");
        props.setProperty("driver.password", "pass");
        props.setProperty("driver.socketFactory", "javax.net.DefaultSocketFactory"); // use for socks5 proxy

        JSQLDataSource dataSource = new JSQLDataSource(props);
        Assert.assertEquals(Dialects.MARIADB.getDriverClassName(), dataSource.getDriverClassName());
        Assert.assertEquals(Dialects.MARIADB, dataSource.getDialect());
        Assert.assertEquals(Integer.parseInt(props.getProperty("loginTimeout")), dataSource.getLoginTimeout());
        Properties driverProps = dataSource.getDriverProperties();
        Assert.assertEquals(props.getProperty("driver.user"), driverProps.getProperty("user"));
        Assert.assertEquals(props.getProperty("driver.password"), driverProps.getProperty("password"));
        Assert.assertEquals(props.getProperty("driver.socketFactory"), driverProps.getProperty("socketFactory"));
    }

    @Test
    public void testDefaultPoolConfig() throws IOException {
        Properties properties = new Properties();
        properties.put("socketFactory", "javax.net.DefaultSocketFactory");

        JSQLDataSource.DataSourceBuilder dataSourceBuilder = JSQLDataSource.newDataSourceBuilder()
                .url(jdbcUrl).user("jsql").password("pass")
                .addDriverProperties(properties);
        JSQLDataSource source = dataSourceBuilder.build();
        try {
            Properties builderProps = dataSourceBuilder.jdbcProperties;
            PoolConfiguration datasourceConf = source.getPoolConfiguration(builderProps);
            PoolConfiguration defaultConf = PoolConfiguration.defaultPoolCfg();

            Assert.assertEquals("javax.net.DefaultSocketFactory", builderProps.getProperty("driver.socketFactory"));
            Assert.assertEquals("javax.net.DefaultSocketFactory", source.getDriverProperties().getProperty("socketFactory"));
            Assert.assertEquals(defaultConf.getMaxPoolSize(), datasourceConf.getMaxPoolSize());
            Assert.assertEquals(defaultConf.getCreateRetryCount(), datasourceConf.getCreateRetryCount());
            Assert.assertEquals(defaultConf.getIdleTimeout(), datasourceConf.getIdleTimeout());
            Assert.assertEquals(defaultConf.getScheduledThreadLifeTime(), datasourceConf.getScheduledThreadLifeTime());
            Assert.assertEquals(defaultConf.getPollTimeout(), datasourceConf.getPollTimeout());
            Assert.assertEquals(defaultConf.isValidateOnBorrow(), datasourceConf.isValidateOnBorrow());
            Assert.assertEquals(defaultConf.isValidateOnReturn(), datasourceConf.isValidateOnReturn());
        } finally {
            source.close();
        }
    }

    @Test
    public void testDataSourceBuilder() throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("driver.socketFactory", "javax.net.DefaultSocketFactory");
        JSQLDataSource.DataSourceBuilder dataSourceBuilder = JSQLDataSource.newDataSourceBuilder()
                .url(jdbcUrl).user("jsql").password("pass").loginTimeout(10)
                .poolMaxSize(8).poolIdleTimeout(500000).poolObjectCreateRetryCount(2).poolPollTimeout(5000)
                .poolScheduleThreadLifeTime(9000).poolValidationOnBorrow(false).poolValidationOnReturn(true)
                .addMapProperties(props);
        JSQLDataSource dataSource = dataSourceBuilder.build();
        try {
            Properties builderProps = dataSourceBuilder.jdbcProperties;
            PoolConfiguration configuration = dataSource.getPoolConfiguration(builderProps);

            Assert.assertEquals(String.valueOf(configuration.getMaxPoolSize()), builderProps.getProperty(JSQLDataSource.PROP_POOL_MAX_POOL_SIZE));
            Assert.assertEquals(String.valueOf(configuration.getIdleTimeout()), builderProps.getProperty(JSQLDataSource.PROP_POOL_IDLE_TIMEOUT));
            Assert.assertEquals(String.valueOf(configuration.getPollTimeout()), builderProps.getProperty(JSQLDataSource.PROP_POOL_POLL_TIMEOUT));
            Assert.assertEquals(String.valueOf(configuration.getCreateRetryCount()), builderProps.getProperty(JSQLDataSource.PROP_POOL_CREATE_RETRY_COUNT));
            Assert.assertEquals(String.valueOf(configuration.getScheduledThreadLifeTime()), builderProps.getProperty(JSQLDataSource.PROP_POOL_SCHEDULED_THREAD_LIFETIME));
            Assert.assertEquals(String.valueOf(configuration.isValidateOnBorrow()), builderProps.getProperty(JSQLDataSource.PROP_POOL_VALIDATE_ON_BORROW));
            Assert.assertEquals(String.valueOf(configuration.isValidateOnReturn()), builderProps.getProperty(JSQLDataSource.PROP_POOL_VALIDATE_ON_RETURN));
            Assert.assertEquals("javax.net.DefaultSocketFactory", builderProps.getProperty("driver.socketFactory"));
            Assert.assertEquals("javax.net.DefaultSocketFactory", dataSource.getDriverProperties().get("socketFactory"));
            Assert.assertEquals(dataSource.getDriverProperties().get(JSQLDataSource.PROP_USER),
                    builderProps.getProperty(JSQLDataSource.PROP_DRIVER_PREFIX + JSQLDataSource.PROP_USER));
            Assert.assertEquals(dataSource.getDriverProperties().get(JSQLDataSource.PROP_PASSWORD),
                    builderProps.getProperty(JSQLDataSource.PROP_DRIVER_PREFIX + JSQLDataSource.PROP_PASSWORD));
            Assert.assertEquals(dataSource.getUrl(), builderProps.getProperty(JSQLDataSource.PROP_URL));
            Assert.assertEquals(String.valueOf(dataSource.getLoginTimeout()), builderProps.getProperty(JSQLDataSource.PROP_LOGIN_TIMEOUT));
            Assert.assertEquals(Dialects.MARIADB.getDialectName(), dataSource.getDialect().getDialectName());
            Assert.assertEquals(Dialects.MARIADB.getDriverClassName(), dataSource.getDialect().getDriverClassName());
        } finally {
            dataSource.close();
        }
    }

    @Test
    public void testInvalidInitDataSource() {
        TestUtils.assertThrows(IllegalArgumentException.class, new ExceptionOperation() {
            @Override
            public void operate() throws Throwable {
                JSQLDataSource.newDataSourceBuilder().url(jdbcUrl).build().close();
            }
        });
        TestUtils.assertThrows(IllegalArgumentException.class, new ExceptionOperation() {
            @Override
            public void operate() throws Throwable {
                JSQLDataSource.newDataSourceBuilder().url(jdbcUrl).password("").build().close();
            }
        });
        TestUtils.assertThrows(NullPointerException.class, new ExceptionOperation() {
            @Override
            public void operate() throws Throwable {
                JSQLDataSource.newDataSourceBuilder().url(jdbcUrl).user("jsql").build().close();
            }
        });
        TestUtils.assertThrows(IllegalArgumentException.class, new ExceptionOperation() {
            @Override
            public void operate() throws Throwable {
                JSQLDataSource.newDataSourceBuilder().dialect("mariadb").user("jsql").password("pass").build().close();
            }
        });
        TestUtils.assertThrows(NullPointerException.class, new ExceptionOperation() {
            @Override
            public void operate() throws Throwable {
                JSQLDataSource.newDataSourceBuilder().user("jsql").password("pass").build().close();
            }
        });
    }
}
