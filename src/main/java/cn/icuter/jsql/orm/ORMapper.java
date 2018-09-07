package cn.icuter.jsql.orm;

import cn.icuter.jsql.ColumnName;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Eq;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * @author edward
 * @since 2018-08-26
 */
public class ORMapper {

    private Object object;

    public ORMapper() {
    }

    public ORMapper(Object object) {
        this.object = object;
    }

    public List<String> getColumns() {
        List<String> colList = new LinkedList<>();
        mapColumn(this.object.getClass(), (col, field) -> colList.add(col));
        return colList;
    }

    public Map<String, Object> toMapIgnoreNullValue() {
        return toMap(true);
    }

    public Map<String, Object> toMap() {
        return toMap(false);
    }

    private Map<String, Object> toMap(boolean ignoreNullValue) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        mapColumn(this.object.getClass(), (col, field) -> {
            try {
                field.setAccessible(true);
                Object v = field.get(this.object);
                if (!ignoreNullValue || v != null) {
                    resultMap.put(col, v);
                }
            } catch (IllegalAccessException e) {
                // TODO log
            }
        });
        return resultMap;
    }

    public void mapColumn(Class<?> clazz, DBColumnMapper dbColumnMapper) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
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

    @FunctionalInterface
    public interface DBColumnMapper {
        void map(String column, Field field);
    }
}
