package cn.icuter.jsql.util;

/**
 * @author edward
 * @since 2018-12-16
 */
public interface RemoveFilter<T> {
    boolean removeIf(T obj);
}
