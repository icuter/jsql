package cn.icuter.jsql.suite;

import cn.icuter.jsql.builder.BuilderTest;
import cn.icuter.jsql.condition.ConditionTest;
import cn.icuter.jsql.data.JSQLBlobTest;
import cn.icuter.jsql.data.JSQLClobTest;
import cn.icuter.jsql.datasource.JSQLDataSourceTest;
import cn.icuter.jsql.orm.ORMapperTest;
import cn.icuter.jsql.pool.DefaultObjectPoolTest;
import cn.icuter.jsql.security.InjectionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author edward
 * @since 2019-02-13
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    JSQLDataSourceTest.class,
    InjectionTest.class,
    BuilderTest.class,
    ORMapperTest.class,
    ConditionTest.class,
    JSQLBlobTest.class,
    JSQLClobTest.class,
    DefaultObjectPoolTest.class
})
public class CommonTestSuite {
}
