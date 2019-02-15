package cn.icuter.jsql.suite;

import cn.icuter.jsql.executor.JdbcExecutorMariaDBTest;
import cn.icuter.jsql.executor.ORMMariaDBTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author edward
 * @since 2019-02-13
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ORMMariaDBTest.class,
    JdbcExecutorMariaDBTest.class
})
public class MariaDBTestSuite {
}
