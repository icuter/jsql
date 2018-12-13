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
}
