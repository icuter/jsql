package cn.icuter.jsql.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.SQLException;

/**
 * @author edward
 * @since 2019-02-06
 */
public class JSQLClob implements java.sql.Clob {

    StringBuilder data = new StringBuilder();

    public JSQLClob() {
    }

    public JSQLClob(String initData) {
        if (initData != null) {
            data.append(initData);
        }
    }

    @Override
    public long length() throws SQLException {
        return data == null ? 0 : data.length();
    }

    @Override
    public String getSubString(long pos, int length) throws SQLException {
        checkPosition(pos);

        int startIndex = (int) pos - 1;
        int endIndex = startIndex + length;
        return data.substring(startIndex, endIndex);
    }

    @Override
    public Reader getCharacterStream() throws SQLException {
        return new StringReader(data.toString());
    }

    @Override
    public InputStream getAsciiStream() throws SQLException {
        return new ByteArrayInputStream(data.toString().getBytes());
    }

    @Override
    public long position(String searchStr, long start) throws SQLException {
        checkPosition(start);

        return data.indexOf(searchStr, (int) start - 1) + 1;
    }

    @Override
    public long position(java.sql.Clob searchStr, long start) throws SQLException {
        String clobString = searchStr.getSubString(1L, (int) searchStr.length());
        return position(clobString, start);
    }

    @Override
    public int setString(long pos, String str) throws SQLException {
        return setString(pos, str, 0, str.length());
    }

    @Override
    public int setString(long pos, String str, int offset, int len) throws SQLException {
        checkPosition(pos);

        String replacing = str.substring(offset, offset + len);
        data.replace((int) pos - 1, (int) pos - 1 + replacing.length(), replacing);
        return replacing.length();
    }

    @Override
    public OutputStream setAsciiStream(long pos) throws SQLException {
        if (pos <= 0 || pos > data.toString().getBytes().length + 1) {
            throw new SQLException("Position Parameter " + pos + " is out of acceptable position between 1 and "
                    + (data.length() + 1));
        }
        return new LobOutputStream(this, (int) pos);
    }

    @Override
    public Writer setCharacterStream(long pos) throws SQLException {
        checkPosition(pos);
        return new ClobWriter(this, (int) pos);
    }

    @Override
    public void truncate(long len) throws SQLException {
        if (len > data.length()) {
            throw new SQLException("Truncated length with " + len + " is grater than existing source data length with "
                    + data.length());
        }
        data.delete((int) len, data.length());
    }

    @Override
    public void free() throws SQLException {
        data.delete(0, data.length());
    }

    @Override
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        return new StringReader(getSubString(pos, (int) length));
    }

    private void checkPosition(long position) throws SQLException {
        if (position <= 0 || position > data.length() + 1) {
            throw new SQLException("Position Parameter " + position + " is out of acceptable position between 1 and "
                    + (data.length() + 1));
        }
    }
}
