package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2019-05-14
 */
public abstract class AbstractDialect implements Dialect {
    public String getDialectName() {
        return null;
    }

    public void injectOffsetLimit(BuilderContext builderCtx) {
        throw new UnsupportedOperationException();
    }

    public boolean supportOffsetLimit() {
        return false;
    }

    public String wrapOffsetLimit(BuilderContext builderContext, String sql) {
        return sql;
    }
    public String wrapLimit(BuilderContext builderContext, String sql) {
        return sql;
    }
    public boolean supportConnectionIsValid() {
        return true;
    }
    public String validationSql() {
        return null;
    }
    public boolean supportSavepoint() {
        return true;
    }
    public boolean supportBlob() {
        return true;
    }
    public boolean supportClob() {
        return true;
    }
    public boolean supportNClob() {
        return true;
    }
    public boolean requireUserPassword() {
        return true;
    }
}
