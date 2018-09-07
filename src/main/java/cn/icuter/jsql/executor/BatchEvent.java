package cn.icuter.jsql.executor;

import java.util.List;

/**
 * @author edward
 * @since 2018-09-06
 */
public class BatchEvent {

    List<BatchResult> result;

    BatchEvent() {
    }

    public List<BatchResult> getResult() {
        return result;
    }
}
