package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.BuilderContext;
import cn.icuter.jsql.orm.ORMapper;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-08-20
 */
public class DefaultJdbcExecutor implements JdbcExecutor {
    private final Connection connection;
    private boolean columnLowerCase;

    public DefaultJdbcExecutor(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int execUpdate(Builder builder) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(builder.getSql())) {
            List<Object> preparedValues = builder.getPreparedValues();
            for (int i = 0, len = preparedValues.size(); i < len; i++) {
                ps.setObject(i + 1, preparedValues.get(i));
            }
            return ps.executeUpdate();
        }
    }

    @Override
    public <T> List<T> execQuery(Builder builder, final Class<T> clazz) throws Exception {
        return doExecQuery(builder, (rs, meta) -> {
            Map<Integer, Field> colIndexFieldMap = mapColumnIndexAndField(clazz, meta);
            List<T> queriedResult = new LinkedList<>();
            while (rs.next()) {
                T record = clazz.newInstance();
                for (Map.Entry<Integer, Field> entry : colIndexFieldMap.entrySet()) {
                    Field field = entry.getValue();
                    field.setAccessible(true);
                    field.set(record, rs.getObject(entry.getKey()));
                }
                queriedResult.add(record);
            }
            return queriedResult;
        });
    }

    @Override
    public List<Map<String, Object>> execQuery(Builder builder) throws Exception {
        return doExecQuery(builder, (rs, meta) -> {
            List<Map<String, Object>> result = new LinkedList<>();
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String colName = meta.getColumnLabel(i);
                    record.put(columnLowerCase ? colName.toLowerCase() : colName, rs.getObject(colName));
                }
                result.add(record);
            }
            return result;
        });
    }

    private <T> T doExecQuery(Builder builder, QueryExecutor<T> queryExecutor) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(builder.getSql())) {
            List<Object> preparedValues = builder.getPreparedValues();
            for (int i = 0, len = preparedValues.size(); i < len; i++) {
                ps.setObject(i + 1, preparedValues.get(i));
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();

            BuilderContext builderContext = builder.getBuilderContext();
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
        }
    }

    @Override
    public void execBatch(List<Builder> builders, BatchCompletedAction completedAction) throws Exception {
        boolean hasCompletedAction = completedAction != null;
        List<BatchResult> batchResultList = new LinkedList<>();
        Map<String, List<Builder>> builderGroup = builders.stream().collect(Collectors.groupingBy(Builder::getSql));
        for (Map.Entry<String, List<Builder>> entry : builderGroup.entrySet()) {
            String sql = entry.getKey();
            List<Builder> valueList = entry.getValue();
            int[] result = execBatch(entry.getKey(), valueList);
            if (hasCompletedAction) {
                batchResultList.add(new BatchResult(sql, result, valueList));
            }
        }
        if (hasCompletedAction) {
            BatchEvent batchEvent = new BatchEvent();
            batchEvent.result = batchResultList;
            completedAction.doAction(batchEvent);
        }
    }

    private int[] execBatch(String sql, List<Builder> builderList) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Builder builder : builderList) {
                List<Object> preparedValues = builder.getPreparedValues();
                for (int i = 0, len = preparedValues.size(); i < len; i++) {
                    ps.setObject(i + 1, preparedValues.get(i));
                }
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    public void setColumnLowerCase(boolean columnLowerCase) {
        this.columnLowerCase = columnLowerCase;
    }

    private Map<Integer, Field> mapColumnIndexAndField(Class<?> clazz, ResultSetMetaData meta) throws SQLException {
        int colLen = meta.getColumnCount();
        List<String> returnColumnList = new ArrayList<>(colLen);
        for (int i = 0; i < colLen; i++) {
            returnColumnList.add(meta.getColumnLabel(i + 1));
        }
        ORMapper mapper = new ORMapper();
        Map<Integer, Field> colFieldMap = new LinkedHashMap<>(returnColumnList.size());
        mapper.mapColumn(clazz, (col, field) -> {
            for (int i = 0; i < returnColumnList.size(); i++) {
                String retColName = returnColumnList.get(i);
                int colIdx = -1;
                if (retColName.equalsIgnoreCase(col)) {
                    colIdx = i;
                }
                if (colIdx >= 0) {
                    colFieldMap.put(colIdx + 1, field);
                }
            }
        });
        return colFieldMap;
    }

    @FunctionalInterface
    interface QueryExecutor<T> {
        T doExec(ResultSet rs, ResultSetMetaData meta) throws Exception;
    }

}
