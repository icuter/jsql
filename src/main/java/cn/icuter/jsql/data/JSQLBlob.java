package cn.icuter.jsql.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author edward
 * @since 2019-02-06
 */
public class JSQLBlob implements java.sql.Blob {
    byte[] data = new byte[0];

    public JSQLBlob() {
    }

    public JSQLBlob(byte[] initData) {
        if (initData != null) {
            data = initData;
        }
    }

    @Override
    public long length() throws SQLException {
        return data.length;
    }

    /**
     * @param pos between 1L(first) and data.length + 1(last)
     */
    @Override
    public byte[] getBytes(long pos, int length) throws SQLException {
        checkPosition(pos);
        return Arrays.copyOfRange(data, (int) pos - 1, (int) pos - 1 + length);
    }

    @Override
    public InputStream getBinaryStream() throws SQLException {
        return new ByteArrayInputStream(data);
    }

    @Override
    public long position(byte[] pattern, long start) throws SQLException {
        checkPosition(start);
        if (pattern.length > data.length) {
            return -1L;
        }
        int from = (int) start - 1;
        byte[] searchRangeBytes = Arrays.copyOfRange(data, from, data.length);
        for (int i = 0; i < searchRangeBytes.length; i++) {
            if (searchRangeBytes.length - i < pattern.length) {
                return -1L;
            }
            for (int j = 0; j < pattern.length; j++) {
                if (searchRangeBytes[i + j] != pattern[j]) {
                    break;
                }
                if (j == pattern.length - 1) {
                    return i + 1;
                }
            }
        }
        return -1L;
    }

    @Override
    public long position(java.sql.Blob pattern, long start) throws SQLException {
        byte[] searchBytes = pattern.getBytes(1L, (int) pattern.length());
        return position(searchBytes, start);
    }

    @Override
    public int setBytes(long pos, byte[] bytes) throws SQLException {
        return setBytes(pos, bytes, 0, bytes.length);
    }

    @Override
    public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
        checkPosition(pos);
        try {
            try (OutputStream outputStream = setBinaryStream(pos)) {
                outputStream.write(bytes, offset, len);
            }
        } catch (IOException e) {
            throw new SQLException(e);
        }
        return len;
    }

    @Override
    public OutputStream setBinaryStream(long pos) throws SQLException {
        checkPosition(pos);
        return new LobOutputStream(this, (int) pos);
    }

    @Override
    public void truncate(long len) throws SQLException {
        if (len > data.length) {
            throw new SQLException("Truncated length with " + len + " is grater than existing source data length with "
                    + data.length);
        }
        data = Arrays.copyOf(data, (int) len);
    }

    @Override
    public void free() throws SQLException {
        data = new byte[0];
    }

    @Override
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        checkPosition(pos);
        int from = (int) pos - 1;
        byte[] partOfBytes = Arrays.copyOfRange(data, from, from + (int) length);
        return new ByteArrayInputStream(partOfBytes);
    }

    private void checkPosition(long position) throws SQLException {
        if (position <= 0 || position > data.length + 1) {
            throw new SQLException("Position Parameter " + position + " is out of acceptable position between 1 and "
                    + (data.length + 1));
        }
    }
}
