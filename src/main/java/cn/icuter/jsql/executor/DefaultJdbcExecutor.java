package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.exception.ExecutionException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.orm.ORMapper;

import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-08-20
 */
public class DefaultJdbcExecutor implements JdbcExecutor {
    private static final JSQLLogger LOGGER = Logs.getLogger(DefaultJdbcExecutor.class);

    final Connection connection;
    private boolean columnLowerCase;

    public DefaultJdbcExecutor(Connection connection) {
        this(connection, true);
    }

    public DefaultJdbcExecutor(Connection connection, boolean columnLowerCase) {
        this.connection = connection;
        this.columnLowerCase = columnLowerCase;
    }

    @Override
    public int execUpdate(Builder builder) throws JSQLException {
        checkAndBuild(builder);
        LOGGER.info("executing sql: " + builder.getSql());
        LOGGER.info("executing values: " + builder.getPreparedValues());
        try (PreparedStatement ps = connection.prepareStatement(builder.getSql())) {
            List<Object> preparedValues = builder.getPreparedValues();
            for (int i = 0, len = preparedValues.size(); i < len; i++) {
                ps.setObject(i + 1, preparedValues.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("executing update error builder detail: " + builder, e);
            throw new ExecutionException("executing update error builder detail: " + builder, e);
        }
    }

    @Override
    public <T> List<T> execQuery(Builder builder, final Class<T> clazz) throws JSQLException {
        return doExecQuery(builder, (rs, meta) -> {
            int fetchSize = rs.getFetchSize();
            boolean hasLimit = fetchSize > 0;
            Map<Field, Integer> colIndexFieldMap = mapColumnFieldAndIndex(clazz, meta);
            List<T> queriedResult = new LinkedList<>();
            while (rs.next()) {
                T record = clazz.newInstance();
                for (Map.Entry<Field, Integer> entry : colIndexFieldMap.entrySet()) {
                    Field field = entry.getKey();
                    int rsIndex = entry.getValue();
                    field.setAccessible(true);
                    if (field.getType().isPrimitive()) {
                        if (field.getType() == Boolean.TYPE) {
                            field.set(record, rs.getBoolean(rsIndex));

                        } else if (field.getType() == Byte.TYPE) {
                            field.set(record, rs.getByte(rsIndex));

                        } else if (field.getType() == Short.TYPE) {
                            field.set(record, rs.getShort(rsIndex));

                        } else if (field.getType() == Integer.TYPE) {
                            field.set(record, rs.getInt(rsIndex));

                        } else if (field.getType() == Long.TYPE) {
                            field.set(record, rs.getLong(rsIndex));

                        } else if (field.getType() == Float.TYPE) {
                            field.set(record, rs.getFloat(rsIndex));

                        } else if (field.getType() == Double.TYPE) {
                            field.set(record, rs.getDouble(rsIndex));

                        } else {
                            field.set(record, rs.getObject(rsIndex, field.getType()));
                        }
                    } else if (Blob.class.isAssignableFrom(field.getType())) {
                        field.set(record, rs.getBlob(rsIndex)); // compatible for mysql/mariaDB
                    } else if (Clob.class.isAssignableFrom(field.getType())) {
                        field.set(record, rs.getClob(rsIndex)); // compatible for mysql
                    } else {
                        field.set(record, rs.getObject(rsIndex, field.getType()));
                    }
                }
                queriedResult.add(record);
                if (hasLimit && --fetchSize <= 0) {
                    break;
                }
            }
            return queriedResult;
        });
    }

    @Override
    public List<Map<String, Object>> execQuery(Builder builder) throws JSQLException {
        return doExecQuery(builder, (rs, meta) -> {
            int fetchSize = rs.getFetchSize();
            boolean hasLimit = fetchSize > 0;
            List<Map<String, Object>> result = new LinkedList<>();
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String colName = meta.getColumnLabel(i);
                    if (colName.toLowerCase().startsWith("rownumber_")) {
                        continue;
                    }
                    colName = columnLowerCase ? colName.toLowerCase() : colName;
                    record.put(colName, rs.getObject(colName));
                }
                result.add(record);
                if (hasLimit && --fetchSize <= 0) {
                    break;
                }
            }
            return result;
        });
    }

    private <T> T doExecQuery(Builder builder, QueryExecutor<T> queryExecutor) throws JSQLException {

        checkAndBuild(builder);

        LOGGER.info("executing query sql: " + builder.getSql());
        LOGGER.info("executing query values: " + builder.getPreparedValues());
        PreparedStatement ps = null;
        BuilderContext builderContext = builder.getBuilderContext();
        try {
            if (!builderContext.getDialect().supportOffsetLimit() && builderContext.getOffset() > 0) {
                ps = connection.prepareStatement(builder.getSql(),
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            } else {
                ps = connection.prepareStatement(builder.getSql());
            }
            List<Object> preparedValues = builder.getPreparedValues();
            for (int i = 0, len = preparedValues.size(); i < len; i++) {
                ps.setObject(i + 1, preparedValues.get(i));
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();

            if (!builderContext.getDialect().supportOffsetLimit()) {
                // like sql paging, must set both offset and limit or limit only
                int offset = builderContext.getOffset();
                if (offset > 0 && builderContext.getLimit() > 0) {
                    rs.absolute(offset);
                }
                if (builderContext.getLimit() > 0) {
                    rs.setFetchSize(builderContext.getLimit());
                }
            }
            return queryExecutor.doExec(rs, meta);
        } catch (Exception e) {
            LOGGER.error("executing query error, builder detail: " + builder, e);
            throw new ExecutionException("executing query error, builder detail: " + builder, e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOGGER.error("closing PreparedStatement error, builder detail: " + builder, e);
                }
            }
        }
    }

    @Override
    public void execBatch(List<Builder> builders) throws JSQLException {
        Map<String, List<Builder>> builderGroup = builders.stream().collect(Collectors.groupingBy(Builder::getSql));
        List<String> sqlList = builders.stream().map(Builder::getSql).collect(Collectors.toList());
        Map<String, List<Builder>> builderOrderGroup = new TreeMap<>(Comparator.comparingInt(sqlList::indexOf));
        builderOrderGroup.putAll(builderGroup);

        for (Map.Entry<String, List<Builder>> entry : builderOrderGroup.entrySet()) {
            execBatch(entry.getKey(), entry.getValue());
        }
    }

    private void execBatch(String sql, List<Builder> builderList) throws JSQLException {
        LOGGER.info("executing batch sql: " + sql);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Builder builder : builderList) {
                checkAndBuild(builder);
                List<Object> preparedValues = builder.getPreparedValues();

                LOGGER.debug("executing batch values: " + preparedValues);

                for (int i = 0, len = preparedValues.size(); i < len; i++) {
                    ps.setObject(i + 1, preparedValues.get(i));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            List<Object> values = builderList.stream().map(Builder::getPreparedValues)
                    .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
            LOGGER.error("executing batch update error, batch sql: " + sql
                    + ", batch values list: \n" + values, e);
            throw new ExecutionException("executing batch update error, batch sql: " + sql
                    + ", batch values list: \n" + values, e);
        }
    }

    public void setColumnLowerCase(boolean columnLowerCase) {
        this.columnLowerCase = columnLowerCase;
    }

    private Map<Field, Integer> mapColumnFieldAndIndex(Class<?> clazz, ResultSetMetaData meta) throws SQLException {
        int colLen = meta.getColumnCount();
        List<String> returnColumnList = new ArrayList<>(colLen);
        for (int i = 0; i < colLen; i++) {
            String colLabel = meta.getColumnLabel(i + 1);
            if (!colLabel.toLowerCase().startsWith("rownumber_")) {
                returnColumnList.add(colLabel);
            }
        }
        Map<Field, Integer> colFieldMap = new LinkedHashMap<>(returnColumnList.size());
        ORMapper.mapColumn(clazz, (col, field) -> {
            for (int i = 0; i < returnColumnList.size(); i++) {
                String retColName = returnColumnList.get(i);
                int colIdx = -1;
                if (retColName.equalsIgnoreCase(col)) {
                    colIdx = i;
                }
                if (colIdx >= 0) {
                    colFieldMap.put(field, colIdx + 1);
                    break;
                }
            }
        });
        return colFieldMap;
    }

    private void checkAndBuild(Builder builder) {
        if (!builder.getBuilderContext().isBuilt()) {
            builder.build();
        }
    }

    @FunctionalInterface
    interface QueryExecutor<T> {
        T doExec(ResultSet rs, ResultSetMetaData meta) throws Exception;
    }

}
