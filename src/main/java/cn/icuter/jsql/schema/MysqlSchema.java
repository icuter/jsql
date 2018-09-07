package cn.icuter.jsql.schema;

import java.util.List;

/**
 * @author edward
 * @since 2018-08-07
 */
public class MysqlSchema implements Schema {

    @Override
    public void init() {

    }

    @Override
    public boolean hasColumn(String columnName) {
        return false;
    }

    @Override
    public boolean hasTable(String tableName) {
        return false;
    }

    @Override
    public List<String> schemas() {
        return null;
    }

    @Override
    public List<String> tables() {
        return null;
    }

    @Override
    public List<String> tablesInSchema(String schemaId) {
        return null;
    }

    @Override
    public List<String> views() {
        return null;
    }

    @Override
    public List<String> viewsInSchema(String schemaId) {
        return null;
    }

    @Override
    public List<String> allColumns() {
        return null;
    }

    @Override
    public List<String> columnsInTable(String tableName) {
        return null;
    }

    @Override
    public List<String> columnsInSchema(String schemaId) {
        return null;
    }
}
