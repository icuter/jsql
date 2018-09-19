package cn.icuter.jsql.executor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author edward
 * @since 2018-09-17
 */
public class CloseableJdbcExecutor extends DefaultJdbcExecutor {

    public CloseableJdbcExecutor(Connection connection) {
        super(connection);
    }

    public CloseableJdbcExecutor(Connection connection, boolean columnLowerCase) {
        super(connection, columnLowerCase);
    }

    @Override
    public void close() throws IOException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IOException("Connection closing error", e);
        }
    }
}
