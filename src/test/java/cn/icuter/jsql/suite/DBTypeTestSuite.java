package cn.icuter.jsql.suite;

import cn.icuter.jsql.executor.JdbcExecutorTest;
import cn.icuter.jsql.executor.JoinTableTest;
import cn.icuter.jsql.executor.ORMTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author edward
 * @since 2019-02-13
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ORMTest.class,
    JdbcExecutorTest.class,
//    JoinTableTest.class,
})
public class DBTypeTestSuite {
}
