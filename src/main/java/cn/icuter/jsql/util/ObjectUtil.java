package cn.icuter.jsql.util;

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

    public static boolean isByteArray(Class<?> type) {
        return isArray(type, "byte");
    }

    public static boolean isArray(Class<?> type, String arrayType) {
        return type.isArray() && arrayType.equalsIgnoreCase(type.getComponentType().getName());
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
