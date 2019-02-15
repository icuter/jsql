package cn.icuter.jsql.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author edward
 * @since 2019-02-07
 */
public class LobOutputStream extends OutputStream {
    private JSQLClob clob;
    private JSQLBlob blob;
    private ByteArrayOutputStream out;

    LobOutputStream(JSQLBlob blob, long position) {
        this.blob = blob;
        init(blob.data, position);
    }

    LobOutputStream(JSQLClob clob, long position) {
        this.clob = clob;
        init(clob.data.toString().getBytes(), position);
    }

    private void init(byte[] srcBytes, long position) {
        this.out = new ByteArrayOutputStream();
        for (int i = 0; i < position - 1; i++) {
            this.out.write(srcBytes[i]);
        }
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void flush() throws IOException {
        if (clob != null) {
            String replacing = out.toString();
            clob.data.replace(0, replacing.length(), replacing);
        }
        if (blob != null) {
            byte[] bytesInStream = out.toByteArray();
            if (bytesInStream.length >= blob.data.length) {
                blob.data = bytesInStream;
            } else {
                out.write(blob.data, bytesInStream.length, blob.data.length - bytesInStream.length);
                blob.data = out.toByteArray();
            }
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
