package cn.icuter.jsql.builder;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author edward
 * @since 2019-02-12
 */
@FunctionalInterface
public interface FieldInterceptor<T> {
    boolean accept(T object, Field field, String colName, Object value, Map<String, Object> resultMap);
}
