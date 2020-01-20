package cn.icuter.jsql.security;

import cn.icuter.jsql.TestUtils;
import org.junit.Test;

public class InjectionTest {

    @Test
    public void testQuoting() {
        Injections.check("\"col--\"", "\"");
        Injections.check("\"col/*\" \"alias/*\"", "\"");
        Injections.check("\"col%\" as \"alias%\"", "\"");
        Injections.check("col \"alias/*\"", "\"");
        Injections.check("col as \"alias%\"", "\"");
        Injections.check("col as \"alias%\"", "\"");
        Injections.check("`t`.`col` as `alias%`", "`");
        Injections.check("t.`col` as `alias%`", "`");
        Injections.check("t.col as `?alias`", "`");
        Injections.check("func(t.col) as `?alias`", "`");
        Injections.check("func(`t`.`col`) as `?alias`", "`");
        Injections.check("(select 1 from dual) as `?alias`", "`");
        Injections.check("(select `t`.`col` from table `t`) as `?alias`", "`");
        Injections.check("`t`.`中文字段名` `中文?别名`", "`");
        Injections.check("`t`.*", "`");
        Injections.check("t.*", "`");
        Injections.check("*", "`");
        Injections.check("", "`");
        Injections.check((String) null, "`");
        Injections.check("col", null);
        Injections.check("col", "");
    }

    @Test
    public void testIllegalField() {
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col--", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col/* \"alias/*\"", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("\"t.col as ?alias\"", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col as alias%", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col as \"alias", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col as 'alias", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col as @alias", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("t--.\"col\" as \"alias\"", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("\"t--.col as alias\"", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("test_func(t.col) as ?alias\"", "\""));
    }
}
