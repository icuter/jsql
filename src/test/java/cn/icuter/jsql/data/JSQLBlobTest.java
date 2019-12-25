package cn.icuter.jsql.data;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        String getString = new String(blob.getBytes(1, (int) blob.length()), "UTF-8");
        Assert.assertEquals(SRC, getString);

        int pos = 6;
        int len = 4;
        byte[] subBytes = blob.getBytes(pos, len);
        Assert.assertEquals(SRC.substring(pos - 1, pos - 1 + len), new String(subBytes, "UTF-8"));
    }

    @Test
    public void getBinaryStream() throws Exception {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        byte[] bytesFromStr = SRC.getBytes("UTF-8");
        int i = 0;
        InputStream in = blob.getBinaryStream();
        try {
            int read;
            while ((read = in.read()) != -1) {
                Assert.assertEquals(bytesFromStr[i++], read);
            }
        } finally {
            in.close();
        }
        i = 0;
        int pos = 6;
        int len = 4;
        in = blob.getBinaryStream(pos, len);
        try {
            byte[] subBytes = SRC.substring(pos - 1, pos - 1 + len).getBytes("UTF-8");
            int read;
            while ((read = in.read()) != -1) {
                Assert.assertEquals(subBytes[i++], read);
            }
        } finally {
            in.close();
        }
    }

    @Test
    public void position() throws Exception {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        int begin = 5;
        int end = 9;
        String subSrc = SRC.substring(begin, end);
        long pos = blob.position(subSrc.getBytes("UTF-8"), 1L);
        Assert.assertEquals(begin + 1, pos);

        pos = blob.position(subSrc.getBytes("UTF-8"), begin + 1);
        Assert.assertEquals(1L, pos);

        pos = blob.position(subSrc.getBytes("UTF-8"), begin + 2);
        Assert.assertEquals(-1L, pos);

        pos = blob.position("not exist".getBytes("UTF-8"), 1L);
        Assert.assertEquals(-1L, pos);

        pos = blob.position(new JSQLBlob(subSrc.getBytes("UTF-8")), 1L);
        Assert.assertEquals(begin + 1, pos);
    }

    @Test
    public void setBytes() throws Exception {
        byte[] testBytes = "icuter".getBytes("UTF-8");
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        int position = 1;
        int written = blob.setBytes(position, testBytes);
        Assert.assertEquals(testBytes.length, written);

        byte[] bytes = blob.getBytes(position, testBytes.length);
        Assert.assertArrayEquals(testBytes, bytes);

        blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        position = 6;
        written = blob.setBytes(position, testBytes);
        Assert.assertEquals(testBytes.length, written);

        bytes = blob.getBytes(position, testBytes.length);
        Assert.assertArrayEquals(testBytes, bytes);

        blob = new JSQLBlob(SRC.getBytes("UTF-8"));
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
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        int removeLen = 5;
        blob.truncate(blob.length() - removeLen);
        Assert.assertEquals(blob.length(), SRC.getBytes("UTF-8").length - removeLen);
        Assert.assertArrayEquals(blob.getBytes(1, (int) blob.length()),
                Arrays.copyOfRange(SRC.getBytes("UTF-8"), 0,
                        SRC.getBytes("UTF-8").length - removeLen));
    }

    @Test(expected = SQLException.class)
    public void getBytesException() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        blob.getBytes(0, (int) blob.length());
    }

    @Test(expected = SQLException.class)
    public void getBinaryStreamException() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        blob.getBinaryStream(0, blob.length());
    }

    @Test(expected = SQLException.class)
    public void positionException() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        blob.position("test".getBytes("UTF-8"), 0);
    }

    @Test(expected = SQLException.class)
    public void positionException2() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        blob.position(new JSQLBlob("test".getBytes("UTF-8")), 0);
    }

    @Test(expected = SQLException.class)
    public void positionException3() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        JSQLBlob searchBlob = new JSQLBlob("test".getBytes("UTF-8"));
        blob.position(searchBlob, blob.length() + 2);
    }

    @Test(expected = SQLException.class)
    public void setBytesException() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        blob.setBytes(0, "icuter".getBytes("UTF-8"));
    }

    @Test(expected = SQLException.class)
    public void setBytesException2() throws SQLException, UnsupportedEncodingException {
        JSQLBlob blob = new JSQLBlob(SRC.getBytes("UTF-8"));
        blob.setBytes(0, "icuter".getBytes("UTF-8"), 2, 2);
    }
}