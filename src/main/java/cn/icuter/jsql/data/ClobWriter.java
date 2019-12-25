package cn.icuter.jsql.data;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author edward
 * @since 2019-02-08
 */
public class ClobWriter extends Writer {
    private final JSQLClob clob;
    private final CharArrayWriter writer;

    ClobWriter(JSQLClob clob, int position) {
        this.clob = clob;
        char[] chars = clob.data.toString().toCharArray();
        this.writer = new CharArrayWriter(chars.length);
        for (int i = 0; i < position - 1; i++) {
            writer.append(chars[i]);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (writer.size() <= clob.data.length()) {
            writer.write(clob.data.toString(), writer.size(), clob.data.length() - writer.size());
        }
        clob.data = new StringBuilder(writer.toString());
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
