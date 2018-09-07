package cn.icuter.jsql.schema;

import java.util.List;

/**
 * @author edward
 * @since 2018-08-07
 */
public interface Schema {

    void init();
    boolean hasColumn(String columnName);
    boolean hasTable(String tableName);
    List<String> schemas();
    List<String> tables();
    List<String> tablesInSchema(String schemaId);
    List<String> views();
    List<String> viewsInSchema(String schemaId);

    List<String> allColumns();
    List<String> columnsInTable(String tableName);
    List<String> columnsInSchema(String schemaId);
}
