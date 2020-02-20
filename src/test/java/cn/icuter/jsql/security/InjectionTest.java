package cn.icuter.jsql.security;

import cn.icuter.jsql.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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
    public void testUpdateWords() {
        InjectionWords.TrieNode root = InjectionWords.getInstance().getRoot();

        Injections.setBlacklistPattern(new String[] {"$"});

        Injections.check("col--", "\"");
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("$col$", "\""));

        List<String> wordList = InjectionWords.getInstance().wordList;
        Assert.assertEquals(1, wordList.size());
        Assert.assertEquals("$", wordList.get(0));
        Assert.assertNotEquals(root, InjectionWords.getInstance().getRoot());

        root = InjectionWords.getInstance().getRoot();
        Injections.setBlacklistPattern(InjectionWords.DEFAULT_WORDS);
        Assert.assertNotEquals(root, InjectionWords.getInstance().getRoot());
        Assert.assertEquals(InjectionWords.DEFAULT_WORDS.length, wordList.size());
        Assert.assertEquals(InjectionWords.DEFAULT_WORDS[InjectionWords.DEFAULT_WORDS.length - 1], wordList.get(wordList.size() - 1));

        root = InjectionWords.getInstance().getRoot();
        Injections.addBlacklistPattern(new String[] {"$"});
        Assert.assertNotEquals(root, InjectionWords.getInstance().getRoot());

        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col--", "\""));
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("$col$", "\""));

        Assert.assertEquals(InjectionWords.DEFAULT_WORDS.length + 1, wordList.size());
        Assert.assertEquals("$", wordList.get(wordList.size() - 1));

        Injections.setBlacklistPattern(InjectionWords.DEFAULT_WORDS);

        Injections.check("$col$", "\"");
        TestUtils.assertThrows(IllegalArgumentException.class, () -> Injections.check("col--", "\""));
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
