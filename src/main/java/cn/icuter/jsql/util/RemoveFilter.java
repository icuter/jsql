package cn.icuter.jsql.util;

/**
 * @author edward
 * @since 2019-05-13
 */
public interface RemoveFilter<T> {
    boolean removeIf(T obj);
}