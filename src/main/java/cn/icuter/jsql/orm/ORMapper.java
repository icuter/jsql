package cn.icuter.jsql.orm;

import cn.icuter.jsql.ColumnName;
import cn.icuter.jsql.builder.FieldInterceptor;
import cn.icuter.jsql.exception.ORMException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author edward
 * @since 2018-08-26
 */
public class ORMapper<T> {

    private T object;

    public ORMapper(T object) {
        this.object = object;
    }

    public static <E> ORMapper<E> of(E object) {
        return new ORMapper<E>(object);
    }

    public Map<String, Object> toMapIgnoreNullValue() {
        return toMap(new FieldInterceptor<T>() {
            @Override
            public boolean accept(T object, Field field, String colName, Object value, Map<String, Object> resultMap) {
                return value != null;
            }
        });
    }

    public Map<String, Object> toMap() {
        return toMap(new FieldInterceptor<T>() {
            @Override
            public boolean accept(T object, Field field, String colName, Object value, Map<String, Object> resultMap) {
                return true;
            }
        });
    }

    public Map<String, Object> toMap(final FieldInterceptor<T> interceptor) {
        final Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        ORMapper.mapColumn(object.getClass(), new DBColumnMapper() {
            @Override
            public void map(String col, Field field) {
                try {
                    field.setAccessible(true);
                    Object v = field.get(object);
                    if (interceptor.accept(object, field, col, v, resultMap)) {
                        resultMap.put(col, v);
                    }
                } catch (IllegalAccessException e) {
                    throw new ORMException("mapping field and column error for col: " + col + " and filed: " + field.getName(), e);
                }
            }
        });
        return resultMap;
    }

    public static void mapColumn(Class<?> clazz, DBColumnMapper dbColumnMapper) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getName().startsWith("this$")) {
                continue;
            }
            String fieldName = null;
            if (field.isEnumConstant() || (field.getModifiers() & Modifier.TRANSIENT) != 0) {
                continue;
            }
            ColumnName fieldAnnotation = field.getAnnotation(ColumnName.class);
            if (fieldAnnotation != null && fieldAnnotation.ignore()) {
                continue;
            }
            if (fieldAnnotation != null && fieldAnnotation.value().length() > 0) {
                fieldName = fieldAnnotation.value();
            }
            if (fieldName == null || fieldName.length() <= 0) {
                fieldName = field.getName();
            }
            dbColumnMapper.map(fieldName, field);
        }
    }

    public interface DBColumnMapper {
        void map(String column, Field field);
    }
}
