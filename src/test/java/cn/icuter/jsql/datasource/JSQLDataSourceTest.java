package cn.icuter.jsql.datasource;

import cn.icuter.jsql.dialect.Dialects;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class JSQLDataSourceTest {

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
        props.setProperty("url", "jdbc:mariadb://192.168.200.96:3307/testdb?serverTimezone=GMT%2B8");
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
}
