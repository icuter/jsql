package cn.icuter.jsql.executor;

import cn.icuter.jsql.builder.Builder;

import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * @author edward
 * @since 2018-09-05
 */
public class BatchResult {

    private Exception exception;
    private int totalCount;
    private int successCount;
    private int failCount;
    private String sql;
    private List<List<Object>> failValues = new LinkedList<>();

    BatchResult(String sql, int[] result, List<Builder> values) {
        this.sql = sql;
        this.totalCount = result.length;
        for (int i = 0; i < result.length; i++) {
            if (result[i] <= 0 && result[i] != Statement.SUCCESS_NO_INFO) {
                failValues.add(values.get(i).getPreparedValues());
                failCount++;
            } else {
                successCount++;
            }
        }
    }

    public boolean hasException() {
        return exception != null;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public String getSql() {
        return sql;
    }

    public List<List<Object>> getFailValues() {
        return failValues;
    }

    public boolean isAllSuccessed() {
        return failCount == 0;
    }

    @Override
    public String toString() {
        return new StringBuilder("BatchResult{")
                .append("totalCount=").append(totalCount)
                .append(", successCount=").append(successCount)
                .append(", failCount=").append(failCount)
                .append(", sql='").append(sql).append('\'')
                .append(", failValues=").append(failValues)
                .append('}').toString();
    }
}
