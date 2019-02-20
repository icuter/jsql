package cn.icuter.jsql.executor;

import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author edward
 * @since 2018-09-17
 */
public class CloseableJdbcExecutor extends DefaultJdbcExecutor {

    private static final JSQLLogger LOGGER = Logs.getLogger(CloseableJdbcExecutor.class);

    public CloseableJdbcExecutor(Connection connection) {
        super(connection);
    }

    @Override
    public void close() throws IOException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.error("Connection closing error", e);
            throw new IOException("Connection closing error", e);
        }
    }
}
