package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;

import java.util.Collection;

public interface BuilderDataSource {
    Builder select(String... cols);
    Builder update(String table);
    Builder insert(String table, String... columns);
    Builder delete();
    Builder sql(String sql, Object... values);
    Builder union(Builder... builders);
    Builder unionAll(Builder... builders);
    Builder union(Collection<Builder> builders);
    Builder unionAll(Collection<Builder> builders);
}
