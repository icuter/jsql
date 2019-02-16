package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.data.JSQLBlob;
import cn.icuter.jsql.data.JSQLClob;
import cn.icuter.jsql.data.JSQLNClob;
import cn.icuter.jsql.exception.ExecutionException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.orm.ORMapper;
import cn.icuter.jsql.util.ObjectUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(builder.getSql());
            List<Object> preparedValues = builder.getPreparedValues();
            for (int i = 0, len = preparedValues.size(); i < len; i++) {
                Object value = preparedValues.get(i);
                if (JSQLClob.class.isAssignableFrom(value.getClass())) {
                    JSQLClob clobValue = (JSQLClob) value;
                    Clob clobFromConnection = connection.createClob();
                    clobFromConnection.setString(1, clobValue.getSubString(1, (int) clobValue.length()));
                    ps.setClob(i + 1, clobFromConnection);
                } else if (JSQLNClob.class.isAssignableFrom(value.getClass())) {
                    JSQLNClob nclobValue = (JSQLNClob) value;
                    NClob nclobFromConnection = connection.createNClob();
                    nclobFromConnection.setString(1, nclobValue.getSubString(1, (int) nclobValue.length()));
                    ps.setNClob(i + 1, nclobFromConnection);
                } else if (JSQLBlob.class.isAssignableFrom(value.getClass())) {
                    JSQLBlob blobValue = (JSQLBlob) value;
                    Blob blobFromConnection = connection.createBlob();
                    blobFromConnection.setBytes(1, blobValue.getBytes(1, (int) blobValue.length()));
                    ps.setBlob(i + 1, blobFromConnection);
                } else {
                    ps.setObject(i + 1, value);
                }
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("executing update error builder detail: " + builder, e);
            throw new ExecutionException("executing update error builder detail: " + builder, e);
        } finally {
            closeSilently(ps);
        }
    }

    @Override
    public <T> List<T> execQuery(Builder builder, final Class<T> clazz) throws JSQLException {
        return doExecQuery(builder, new QueryExecutor<List<T>>() {
            @Override
            public List<T> doExec(ResultSet rs, ResultSetMetaData meta) throws Exception {
                int fetchSize = rs.getFetchSize();
                boolean hasLimit = fetchSize > 0;
                Map<Field, Integer> colIndexFieldMap = mapColumnFieldAndIndex(clazz, meta);
                List<T> queriedResult = new LinkedList<T>();
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
                                field.set(record, rs.getObject(rsIndex));
                            }
                        } else if (Blob.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getBlob(rsIndex));
                        } else if (NClob.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getNClob(rsIndex));
                        } else if (Clob.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getClob(rsIndex));
                        } else if (ObjectUtil.isByteArray(field)) {
                            field.set(record, rs.getBytes(rsIndex));
                        } else if (Boolean.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getBoolean(rsIndex));
                        } else if (Byte.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getByte(rsIndex));
                        } else if (Short.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getShort(rsIndex));
                        } else if (Integer.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getInt(rsIndex));
                        } else if (Long.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getLong(rsIndex));
                        } else if (Float.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getFloat(rsIndex));
                        } else if (Double.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getDouble(rsIndex));
                        } else if (String.class.isAssignableFrom(field.getType())) {
                            field.set(record, rs.getString(rsIndex));
                        } else {
                            field.set(record, rs.getObject(rsIndex));
                        }
                    }
                    queriedResult.add(record);
                    if (hasLimit && --fetchSize <= 0) {
                        break;
                    }
                }
                return queriedResult;
            }
        });
    }

    @Override
    public List<Map<String, Object>> execQuery(Builder builder) throws JSQLException {
        return doExecQuery(builder, new QueryExecutor<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> doExec(ResultSet rs, ResultSetMetaData meta) throws Exception {
                int fetchSize = rs.getFetchSize();
                boolean hasLimit = fetchSize > 0;
                List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
                while (rs.next()) {
                    Map<String, Object> record = new LinkedHashMap<String, Object>();
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
            }
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
            closeSilently(ps);
        }
    }

    @Override
    public void execBatch(List<Builder> builders) throws JSQLException {
        Map<String, List<Builder>> builderGroup = new HashMap<String, List<Builder>>();
        for (Builder builder : builders) {
            List<Builder> builderList = builderGroup.get(builder.getSql());
            if (builderList == null) {
                builderList = new LinkedList<Builder>();
                builderList.add(builder);
                builderGroup.put(builder.getSql(), builderList);
            } else {
                builderList.add(builder);
            }
        }
        final List<String> sqlList = new LinkedList<String>();
        for (Builder builder : builders) {
            sqlList.add(builder.getSql());
        }
        Map<String, List<Builder>> builderOrderGroup = new TreeMap<String, List<Builder>>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int first = sqlList.indexOf(o1);
                int second = sqlList.indexOf(o2);
                return (first < second) ? -1 : ((first == second) ? 0 : 1);
            }
        });
        builderOrderGroup.putAll(builderGroup);

        for (Map.Entry<String, List<Builder>> entry : builderOrderGroup.entrySet()) {
            execBatch(entry.getKey(), entry.getValue());
        }
    }

    private void execBatch(String sql, List<Builder> builderList) throws JSQLException {
        LOGGER.info("executing batch sql: " + sql);
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
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
            List<Object> values = new LinkedList<Object>();
            for (Builder builder : builderList) {
                values.add(builder.getPreparedValues());
            }
            LOGGER.error("executing batch update error, batch sql: " + sql
                    + ", batch values list: \n" + values, e);
            throw new ExecutionException("executing batch update error, batch sql: " + sql
                    + ", batch values list: \n" + values, e);
        } finally {
            closeSilently(ps);
        }
    }

    public void setColumnLowerCase(boolean columnLowerCase) {
        this.columnLowerCase = columnLowerCase;
    }

    private Map<Field, Integer> mapColumnFieldAndIndex(Class<?> clazz, ResultSetMetaData meta) throws SQLException {
        int colLen = meta.getColumnCount();
        final List<String> returnColumnList = new ArrayList<String>(colLen);
        for (int i = 0; i < colLen; i++) {
            String colLabel = meta.getColumnLabel(i + 1);
            if (!colLabel.toLowerCase().startsWith("rownumber_")) {
                returnColumnList.add(colLabel);
            }
        }
        final Map<Field, Integer> colFieldMap = new LinkedHashMap<Field, Integer>(returnColumnList.size());
        ORMapper.mapColumn(clazz, new ORMapper.DBColumnMapper() {
            @Override
            public void map(String col, Field field) {
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
            }
        });
        return colFieldMap;
    }

    private void checkAndBuild(Builder builder) {
        if (!builder.getBuilderContext().isBuilt()) {
            builder.build();
        }
    }

    public void closeSilently(PreparedStatement ps) {
        try {
            if (ps != null && !ps.isClosed()) {
                ps.close();
            }
        } catch (SQLException e) {
            LOGGER.warn("closing PreparedStatement error", e);
        }
    }

    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    interface QueryExecutor<T> {
        T doExec(ResultSet rs, ResultSetMetaData meta) throws Exception;
    }

}
