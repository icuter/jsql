package cn.icuter.jsql.util;

import java.util.Iterator;

/**
 * @author edward
 * @since 2018-12-11
 */
public abstract class CollectionUtil {

    public static String join(String[] src, String delimiter) {
        StringBuilder joinBuilder = new StringBuilder();
        for (String s : src) {
            joinBuilder.append(s).append(delimiter);
        }
        return joinBuilder.toString().replaceFirst(delimiter + "\\s*$", "");
    }

    public static <T> void iterate(Iterable<T> iterable, RemoveFilter<T> filter) {
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            if (filter.removeIf(obj)) {
                iterator.remove();
            }
        }
    }

    public interface RemoveFilter<T> {
        boolean removeIf(T obj);
    }
}
