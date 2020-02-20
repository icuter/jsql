package cn.icuter.jsql.security;

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

        Injections.setBlacklistPattern(new String[] {"'"});

        List<String> wordList = InjectionWords.getInstance().wordList;
        Assert.assertEquals(1, wordList.size());
        Assert.assertEquals("'", wordList.get(0));
        Assert.assertNotEquals(root, InjectionWords.getInstance().getRoot());

        root = InjectionWords.getInstance().getRoot();
        Injections.setBlacklistPattern(InjectionWords.DEFAULT_WORDS);
        Assert.assertNotEquals(root, InjectionWords.getInstance().getRoot());

        root = InjectionWords.getInstance().getRoot();
        Injections.addBlacklistPattern(new String[] {"'"});
        Assert.assertNotEquals(root, InjectionWords.getInstance().getRoot());

        Assert.assertEquals(InjectionWords.DEFAULT_WORDS.length + 1, wordList.size());
        Assert.assertEquals("'", wordList.get(wordList.size() - 1));

        Injections.setBlacklistPattern(InjectionWords.DEFAULT_WORDS);
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
    public void testIllegalAlias2() {
        Injections.check("\"t.col as ?alias\"", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAs() {
        Injections.check("col as alias%", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalQuote() {
        Injections.check("col as \"alias", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalQuote2() {
        Injections.check("col as 'alias", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalBracket() {
        Injections.check("col as @alias", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTable() {
        Injections.check("t--.\"col\" as \"alias\"", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTable2() {
        Injections.check("\"t--.col as alias\"", "\"");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFunc() {
        Injections.check("test_func(t.col) as ?alias\"", "\"");
    }
}
