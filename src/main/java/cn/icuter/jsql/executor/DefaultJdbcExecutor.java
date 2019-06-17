package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.data.JSQLBlob;
import cn.icuter.jsql.data.JSQLClob;
import cn.icuter.jsql.data.JSQLNClob;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.exception.ExecutionException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.log.JSQLLogger;
import cn.icuter.jsql.log.Logs;
import cn.icuter.jsql.orm.ORMapper;
import cn.icuter.jsql.util.ObjectUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
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

    protected final Connection connection;

    public DefaultJdbcExecutor(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int execUpdate(Builder builder) throws JSQLException {
        checkAndBuild(builder);
        LOGGER.info("executing sql: " + builder.getSql());
        LOGGER.debug("executing values: " + builder.getPreparedValues());
        PreparedStatement ps = null;
        try {
            try {
                ps = connection.prepareStatement(builder.getSql());
                setPreparedStatementValues(ps, builder);
                return ps.executeUpdate();
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("executing update error builder detail: " + builder, e);
            throw new ExecutionException("executing update error builder detail: " + builder, e);
        }
    }

    private void setPreparedStatementValues(PreparedStatement ps, Builder builder) throws SQLException {
        Dialect dialect = builder.getBuilderContext().getDialect();
        List<Object> preparedValues = builder.getPreparedValues();
        for (int i = 0, len = preparedValues.size(); i < len; i++) {
            Object value = preparedValues.get(i);
            int paramIndex = i + 1;
            if (JSQLNClob.class.isAssignableFrom(value.getClass())) {
                if (dialect.supportNClob()) {
                    ps.setNClob(paramIndex, ((JSQLNClob) value).copyTo(connection.createNClob()));
                } else {
                    // usually, if driver do not support NClob, would do not support NString operation as well
                    // NString operation like getNString or setNString
                    ps.setString(paramIndex, ((JSQLNClob) value).getNClobString());
                }
            } else if (JSQLClob.class.isAssignableFrom(value.getClass())) {
                if (dialect.supportClob()) {
                    ps.setClob(paramIndex, ((JSQLClob) value).copyTo(connection.createClob()));
                } else {
                    ps.setString(paramIndex, ((JSQLClob) value).getClobString());
                }
            } else if (JSQLBlob.class.isAssignableFrom(value.getClass())) {
                if (dialect.supportBlob()) {
                    ps.setBlob(paramIndex, ((JSQLBlob) value).copyTo(connection.createBlob()));
                } else {
                    ps.setBytes(paramIndex, ((JSQLBlob) value).getBlobBytes());
                }
            } else {
                ps.setObject(paramIndex, value);
            }
        }
    }

    @Override
    public <T> List<T> execQuery(final Builder builder, final Class<T> clazz) throws JSQLException {
        return doExecQuery(builder, new QueryExecutor<List<T>>() {
            @Override
            public List<T> doExec(ResultSet rs, ResultSetMetaData meta) throws Exception {
                Dialect dialect = builder.getBuilderContext().getDialect();
                int fetchSize = rs.getFetchSize();
                boolean hasLimit = !dialect.supportOffsetLimit() && fetchSize > 0;
                Map<Field, Integer> colIndexFieldMap = mapColumnFieldAndIndex(clazz, meta);
                List<T> queriedResult = new LinkedList<T>();
                while (rs.next()) {
                    T record = clazz.newInstance();
                    for (Map.Entry<Field, Integer> entry : colIndexFieldMap.entrySet()) {
                        Field field = entry.getKey();
                        int rsIndex = entry.getValue();
                        field.setAccessible(true);
                        field.set(record, getValueByType(field.getType(), rs, rsIndex));
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

    private Object getValueByType(Class<?> type, ResultSet rs, int rsIndex) throws SQLException {
        if (type.isPrimitive()) {
            if (type == Boolean.TYPE) {
                return rs.getBoolean(rsIndex);
            } else if (type == Byte.TYPE) {
                return rs.getByte(rsIndex);
            } else if (type == Short.TYPE) {
                return rs.getShort(rsIndex);
            } else if (type == Integer.TYPE) {
                return rs.getInt(rsIndex);
            } else if (type == Long.TYPE) {
                return rs.getLong(rsIndex);
            } else if (type == Float.TYPE) {
                return rs.getFloat(rsIndex);
            } else if (type == Double.TYPE) {
                return rs.getDouble(rsIndex);
            } else {
                return rs.getObject(rsIndex);
            }
        } else if (Blob.class.isAssignableFrom(type)) {
            return new JSQLBlob(rs.getBytes(rsIndex));
        } else if (NClob.class.isAssignableFrom(type)) {
            return new JSQLNClob(rs.getNString(rsIndex));
        } else if (Clob.class.isAssignableFrom(type)) {
            return new JSQLClob(rs.getString(rsIndex));
        } else if (ObjectUtil.isByteArray(type)) {
            return rs.getBytes(rsIndex);
        } else if (Boolean.class.isAssignableFrom(type)) {
            return rs.getBoolean(rsIndex);
        } else if (Byte.class.isAssignableFrom(type)) {
            return rs.getByte(rsIndex);
        } else if (Short.class.isAssignableFrom(type)) {
            return rs.getShort(rsIndex);
        } else if (Integer.class.isAssignableFrom(type)) {
            return rs.getInt(rsIndex);
        } else if (Long.class.isAssignableFrom(type)) {
            return rs.getLong(rsIndex);
        } else if (Float.class.isAssignableFrom(type)) {
            return rs.getFloat(rsIndex);
        } else if (Double.class.isAssignableFrom(type)) {
            return rs.getDouble(rsIndex);
        } else if (String.class.isAssignableFrom(type)) {
            return rs.getString(rsIndex);
        } else if (BigDecimal.class.isAssignableFrom(type)) {
            return rs.getBigDecimal(rsIndex);
        } else {
            return rs.getObject(rsIndex);
        }
    }

    @Override
    public List<Map<String, Object>> execQuery(final Builder builder) throws JSQLException {
        return doExecQuery(builder, new QueryExecutor<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> doExec(ResultSet rs, ResultSetMetaData meta) throws Exception {
                Dialect dialect = builder.getBuilderContext().getDialect();
                int fetchSize = rs.getFetchSize();
                boolean hasLimit = !dialect.supportOffsetLimit() && fetchSize > 0;
                List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
                while (rs.next()) {
                    Map<String, Object> record = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String colName = meta.getColumnLabel(i).toLowerCase();
                        if (colName.startsWith("rownumber_")) {
                            continue;
                        }
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
        LOGGER.debug("executing query values: " + builder.getPreparedValues());
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
        Map<String, List<Builder>> builderGroup = new LinkedHashMap<String, List<Builder>>();
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

    @Override
    public void close() throws IOException {
        // noop
    }

    private void execBatch(String sql, List<Builder> builderList) throws JSQLException {
        LOGGER.info("executing batch sql: " + sql);
        PreparedStatement ps = null;
        try {
            try {
                ps = connection.prepareStatement(sql);
                for (Builder builder : builderList) {
                    checkAndBuild(builder);

                    LOGGER.debug("executing batch values: " + builder.getPreparedValues());

                    setPreparedStatementValues(ps, builder);
                    ps.addBatch();
                }
                ps.executeBatch();
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        } catch (SQLException e) {
            List<Object> values = new LinkedList<Object>();
            for (Builder builder : builderList) {
                values.add(builder.getPreparedValues());
            }
            LOGGER.error("executing batch update error, batch sql: " + sql
                    + ", batch values list: \n" + values, e);
            throw new ExecutionException("executing batch update error, batch sql: " + sql
                    + ", batch values list: \n" + values, e);
        }
    }

    private Map<Field, Integer> mapColumnFieldAndIndex(Class<?> clazz, ResultSetMetaData meta) throws SQLException {
        int colLen = meta.getColumnCount();
        final List<String> returnColumnList = new ArrayList<String>(colLen);
        for (int i = 0; i < colLen; i++) {
            returnColumnList.add(meta.getColumnLabel(i + 1));
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
        if (!builder.getBuilderContext().hasBuilt()) {
            builder.build();
        }
    }

    interface QueryExecutor<T> {
        T doExec(ResultSet rs, ResultSetMetaData meta) throws Exception;
    }
}
