package cn.icuter.jsql.data;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author edward
 * @since 2019-02-09
 */
public class JSQLBlobTest {
    private static final String SRC = "test jsql blob";
    @Test
    public void getBytes() throws Exception {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        String getString = new String(blob.getBytes(1, (int) blob.length()));
        Assert.assertEquals(SRC, getString);

        int pos = 6;
        int len = 4;
        byte[] subBytes = blob.getBytes(pos, len);
        Assert.assertEquals(SRC.substring(pos - 1, pos - 1 + len), new String(subBytes));
    }

    @Test
    public void getBinaryStream() throws Exception {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        byte[] bytesFromStr = SRC.getBytes();
        int i = 0;
        try (InputStream in = blob.getBinaryStream()) {
            int read;
            while ((read = in.read()) != -1) {
                Assert.assertEquals(bytesFromStr[i++], read);
            }
        }
        i = 0;
        int pos = 6;
        int len = 4;
        try (InputStream in = blob.getBinaryStream(pos, len)) {
            byte[] subBytes = SRC.substring(pos - 1, pos - 1 + len).getBytes();
            int read;
            while ((read = in.read()) != -1) {
                Assert.assertEquals(subBytes[i++], read);
            }
        }
    }

    @Test
    public void position() throws Exception {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        int begin = 5;
        int end = 9;
        String subSrc = SRC.substring(begin, end);
        long pos = blob.position(subSrc.getBytes(), 1L);
        Assert.assertEquals(begin + 1, pos);

        pos = blob.position(subSrc.getBytes(), begin + 1);
        Assert.assertEquals(1L, pos);

        pos = blob.position(subSrc.getBytes(), begin + 2);
        Assert.assertEquals(-1L, pos);

        pos = blob.position("not exist".getBytes(), 1L);
        Assert.assertEquals(-1L, pos);

        pos = blob.position(new JSQLBlob(subSrc.getBytes()), 1L);
        Assert.assertEquals(begin + 1, pos);
    }

    @Test
    public void setBytes() throws Exception {
        byte[] testBytes = "icuter".getBytes();
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        int position = 1;
        int written = blob.setBytes(position, testBytes);
        Assert.assertEquals(testBytes.length, written);

        byte[] bytes = blob.getBytes(position, testBytes.length);
        Assert.assertArrayEquals(testBytes, bytes);

        blob = new JSQLBlob(SRC.getBytes());
        position = 6;
        written = blob.setBytes(position, testBytes);
        Assert.assertEquals(testBytes.length, written);

        bytes = blob.getBytes(position, testBytes.length);
        Assert.assertArrayEquals(testBytes, bytes);

        blob = new JSQLBlob(SRC.getBytes());
        position = (int) blob.length() + 1; // append at last
        written = blob.setBytes(position, testBytes);
        Assert.assertEquals(testBytes.length, written);

        bytes = blob.getBytes(position, testBytes.length);
        Assert.assertArrayEquals(testBytes, bytes);
    }

    @Test
    public void setBinaryStream() throws Exception {
        // tested from setBytes()
    }

    @Test
    public void truncate() throws Exception {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        int removeLen = 5;
        blob.truncate(blob.length() - removeLen);
        Assert.assertEquals(blob.length(), SRC.getBytes().length - removeLen);
        Assert.assertArrayEquals(blob.getBytes(1, (int) blob.length()),
                Arrays.copyOfRange(SRC.getBytes(), 0, SRC.getBytes().length - removeLen));
    }

    @Test(expected = SQLException.class)
    public void getBytesException() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        blob.getBytes(0, (int) blob.length());
    }

    @Test(expected = SQLException.class)
    public void getBinaryStreamException() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        blob.getBinaryStream(0, blob.length());
    }

    @Test(expected = SQLException.class)
    public void positionException() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        blob.position("test".getBytes(), 0);
    }

    @Test(expected = SQLException.class)
    public void positionException2() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        blob.position(new JSQLBlob("test".getBytes()), 0);
    }

    @Test(expected = SQLException.class)
    public void positionException3() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        JSQLBlob searchBlob = new JSQLBlob("test".getBytes());
        blob.position(searchBlob, blob.length() + 2);
    }

    @Test(expected = SQLException.class)
    public void setBytesException() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        blob.setBytes(0, "icuter".getBytes());
    }

    @Test(expected = SQLException.class)
    public void setBytesException2() throws SQLException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes());
        blob.setBytes(0, "icuter".getBytes(), 2, 2);
    }
}