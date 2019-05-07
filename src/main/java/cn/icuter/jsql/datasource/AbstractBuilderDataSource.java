package cn.icuter.jsql.datasource;

import cn.icuter.jsql.builder.Builder;
import cn.icuter.jsql.builder.DeleteBuilder;
import cn.icuter.jsql.builder.InsertBuilder;
import cn.icuter.jsql.builder.SQLBuilder;
import cn.icuter.jsql.builder.SelectBuilder;
import cn.icuter.jsql.builder.UnionSelectBuilder;
import cn.icuter.jsql.builder.UpdateBuilder;
import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.executor.JdbcExecutor;

import java.util.Collection;

public abstract class AbstractBuilderDataSource implements BuilderDataSource {

    public abstract JdbcExecutor provideExecutor();
    public abstract Dialect provideDialect();

    @Override
    public Builder select(String... cols) {
        return new ExecutableSelectBuilder(provideDialect()).select(cols);
    }
    @Override
    public Builder update(String table) {
        return new ExecutableUpdateBuilder(provideDialect()).update(table);
    }
    @Override
    public Builder insert(String table, String... columns) {
        return new ExecutableInsertBuilder(provideDialect()).insert(table, columns);
    }
    @Override
    public Builder delete() {
        return new ExecutableDeleteBuilder(provideDialect()).delete();
    }
    @Override
    public Builder sql(String sql, Object... values) {
        return new ExecutableSQLBuilder().sql(sql).value(values);
    }
    @Override
    public Builder union(Builder... builders) {
        return new ExecutableUnionSelectBuilder(provideDialect(), false, builders);
    }
    @Override
    public Builder unionAll(Builder... builders) {
        return new ExecutableUnionSelectBuilder(provideDialect(), true, builders);
    }
    @Override
    public Builder union(Collection<Builder> builders) {
        return new ExecutableUnionSelectBuilder(provideDialect(), false, builders);
    }
    @Override
    public Builder unionAll(Collection<Builder> builders) {
        return new ExecutableUnionSelectBuilder(provideDialect(), true, builders);
    }

    class ExecutableSelectBuilder extends SelectBuilder {
        ExecutableSelectBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableUpdateBuilder extends UpdateBuilder {
        ExecutableUpdateBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableInsertBuilder extends InsertBuilder {
        ExecutableInsertBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableDeleteBuilder extends DeleteBuilder {
        ExecutableDeleteBuilder(Dialect dialect) {
            super(dialect);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableSQLBuilder extends SQLBuilder {
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }
    class ExecutableUnionSelectBuilder extends UnionSelectBuilder {
        ExecutableUnionSelectBuilder(Dialect dialect, boolean isUnionAll, Builder... builders) {
            super(dialect, isUnionAll, builders);
        }
        ExecutableUnionSelectBuilder(Dialect dialect, boolean isUnionAll, Collection<Builder> builders) {
            super(dialect, isUnionAll, builders);
        }
        @Override
        protected JdbcExecutor provideClosableExecutor() {
            return provideExecutor();
        }
    }

}
