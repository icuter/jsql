package cn.icuter.jsql.security;

import org.junit.Test;

public class InjectionTest {

    @Test
    public void testQuoting() {
        Injections.check("\"col--\"", "\"");
        Injections.check("\"col/*\" \"alias/*\"", "\"");
        Injections.check("\"col%\" as \"alias%\"", "\"");
        Injections.check("col \"alias/*\"", "\"");
        Injections.check("col as \"alias%\"", "\"");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalField() {
        Injections.check("col--", "\"");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAlias() {
        Injections.check("col/* \"alias/*\"", "\"");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAs() {
        Injections.check("col as alias%", "\"");
    }
}
