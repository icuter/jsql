package cn.icuter.jsql.suite;

import cn.icuter.jsql.executor.JdbcExecutorOracleTest;
import cn.icuter.jsql.executor.ORMOracleTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author edward
 * @since 2019-02-13
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ORMOracleTest.class,
    JdbcExecutorOracleTest.class
})
public class OracleTestSuite {
}
