package cn.icuter.jsql.data;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author edward
 * @since 2019-02-10
 */
public class JSQLClobTest {
    private static final String SRC = "test jsql clob";
    @Test
    public void length() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        Assert.assertEquals(SRC.length(), clob.length());
    }

    @Test
    public void getSubString() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        int position = 1;
        int len = (int) clob.length();
        String subString = clob.getSubString(position, len);
        Assert.assertEquals(SRC, subString);

        position = 6;
        len = 4;
        subString = clob.getSubString(position, len);
        Assert.assertEquals(SRC.substring(position - 1, position - 1 + len), subString);

        position = (int) clob.length();
        len = 1;
        subString = clob.getSubString(position, len);
        Assert.assertEquals(SRC.substring(position - 1, position - 1 + len), subString);
    }

    @Test
    public void getCharacterStream() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        char[] chars = new char[(int) clob.length()];
        try (Reader reader = clob.getCharacterStream()) {
            Assert.assertEquals(clob.length(), reader.read(chars));
        }
        assertArrayEquals(chars, SRC.toCharArray());

        int position = 6;
        int len = 4;
        chars = new char[4];
        try (Reader reader = clob.getCharacterStream(position, len)) {
            Assert.assertEquals(len, reader.read(chars));
        }
        assertArrayEquals(chars, SRC.substring(position - 1, position - 1 + len).toCharArray());
    }

    @Test
    public void getAsciiStream() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        byte[] srcBytes = SRC.getBytes();
        byte[] readBytes = new byte[srcBytes.length];
        try (InputStream in = clob.getAsciiStream()) {
            assertEquals(srcBytes.length, in.read(readBytes));
        }
        assertArrayEquals(srcBytes, readBytes);
    }

    @Test
    public void position() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        String searchStr = "jsql";
        Assert.assertEquals(SRC.indexOf(searchStr) + 1, clob.position("jsql", 1L));

        JSQLClob searchClob = new JSQLClob(searchStr);
        Assert.assertEquals(SRC.indexOf(searchStr) + 1, clob.position(searchClob, 1L));

        searchStr = "测试";
        searchClob = new JSQLClob(searchStr);
        Assert.assertEquals(SRC.indexOf(searchStr) + 1, clob.position(searchClob, 1L));
    }

    @Test
    public void setString() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        String setStr = " icuter";
        Assert.assertEquals(setStr.length(), clob.setString(clob.length() + 1, setStr));
        Assert.assertEquals(SRC + setStr, clob.getSubString(1L, (int) clob.length()));

        Assert.assertEquals(setStr.length(), clob.setString(6, setStr));
        Assert.assertEquals(setStr, clob.getSubString(6L, setStr.length()));
    }

    @Test
    public void setAsciiStream() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        String setStr = "icuter";
        try (OutputStream out = clob.setAsciiStream(1L)) {
            out.write(setStr.getBytes());
        }
        Assert.assertEquals(SRC.length(), clob.length());
        Assert.assertEquals(SRC.replaceFirst("^.{" + setStr.length() + "}", setStr),
                clob.getSubString(1L, (int) clob.length()));

        int pos = 6;
        clob = new JSQLClob(SRC);
        try (OutputStream out = clob.setAsciiStream(pos)) {
            out.write(setStr.getBytes());
        }
        Assert.assertEquals(SRC.length(), clob.length());
        Assert.assertEquals("test " + setStr + "lob", clob.getSubString(1L, (int) clob.length()));

        pos = SRC.getBytes().length + 1;
        clob = new JSQLClob(SRC);
        try (OutputStream out = clob.setAsciiStream(pos)) {
            out.write(setStr.getBytes());
        }
        Assert.assertEquals(SRC.length() + setStr.length(), clob.length());
        Assert.assertEquals((SRC + setStr), clob.getSubString(1L, (int) clob.length()));
    }

    @Test
    public void setCharacterStream() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        String setStr = "icuter";
        try (Writer writer = clob.setCharacterStream(1L)) {
            writer.write(setStr);
        }
        Assert.assertEquals(SRC.length(), clob.length());
        Assert.assertEquals(SRC.replaceFirst("^.{" + setStr.length() + "}", setStr),
                clob.getSubString(1L, (int) clob.length()));

        clob = new JSQLClob(SRC);
        try (Writer writer = clob.setCharacterStream(clob.length() + 1)) {
            writer.write(setStr);
        }
        Assert.assertEquals(SRC.length() + setStr.length(), clob.length());
        Assert.assertEquals((SRC + setStr), clob.getSubString(1L, (int) clob.length()));

    }

    @Test
    public void truncate() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        int len = 10;
        clob.truncate(len);
        Assert.assertEquals(len, clob.length());
        Assert.assertEquals(SRC.substring(0, len), clob.getSubString(1L, len));
    }

    @Test
    public void free() throws Exception {
        JSQLClob clob = new JSQLClob(SRC);
        clob.free();
        Assert.assertEquals(0L, clob.length());
    }
    @Test(expected = SQLException.class)
    public void getSubStringException() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        clob.getSubString(0, (int) clob.length());
    }

    @Test(expected = SQLException.class)
    public void getCharacterStreamException() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        clob.getCharacterStream(0, clob.length());
    }

    @Test(expected = SQLException.class)
    public void positionException() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        clob.position("test", 0);
    }

    @Test(expected = SQLException.class)
    public void positionException2() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        clob.position(new JSQLClob("test"), 0);
    }

    @Test(expected = SQLException.class)
    public void positionException3() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        JSQLClob searchBlob = new JSQLClob("test");
        clob.position(searchBlob, clob.length() + 2);
    }

    @Test(expected = SQLException.class)
    public void setStringException() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        clob.setString(0, "icuter");
    }

    @Test(expected = SQLException.class)
    public void setStringException2() throws SQLException {
        JSQLClob clob = new JSQLClob(SRC);
        clob.setString(0, "icuter", 2, 2);
    }
}