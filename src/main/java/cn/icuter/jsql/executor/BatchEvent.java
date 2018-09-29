package cn.icuter.jsql.executor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-09-06
 */
public class BatchEvent {

    Exception exception;
    List<BatchResult> result;
    Map<String, BatchResult> resultMap;

    BatchEvent(List<BatchResult> result) {
        this.result = result;
        this.resultMap = result.stream().collect(Collectors.toMap(BatchResult::getSql, Function.identity()));
    }

    public List<BatchResult> getResult() {
        return result;
    }

    public Map<String, BatchResult> getSQLResultMap() {
        return resultMap;
    }

    private boolean hasException() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }
}
