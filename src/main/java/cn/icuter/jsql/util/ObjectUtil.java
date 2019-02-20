package cn.icuter.jsql.util;

import java.lang.reflect.Field;

/**
 * @author edward
 * @since 2018-12-11
 */
public abstract class ObjectUtil {

    public static void requireNonNull(Object object, String message) {
        if (object == null) {
            throw new NullPointerException(message == null ? "" : message);
        }
    }

    public static void requireNonNull(Object object) {
        requireNonNull(object, null);
    }

    public static boolean isByteArray(Field field) {
        return isArray(field, "byte");
    }

    public static boolean isArray(Field field, String arrayType) {
        return field.getType().isArray() && arrayType.equalsIgnoreCase(field.getType().getComponentType().getName());
    }

    public static void requireNonEmpty(String src, String message) {
        if (src == null || src.length() <= 0) {
            throw new IllegalArgumentException(message == null ? "argument is empty" : message);
        }
    }

    public static void requireNonEmpty(String src) {
        requireNonEmpty(src, null);
    }
}
