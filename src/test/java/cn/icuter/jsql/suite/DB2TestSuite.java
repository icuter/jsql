package cn.icuter.jsql.suite;

import cn.icuter.jsql.executor.JdbcExecutorDB2Test;
import cn.icuter.jsql.executor.ORMDB2Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author edward
 * @since 2019-02-13
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ORMDB2Test.class,
    JdbcExecutorDB2Test.class
})
public class DB2TestSuite {
}
