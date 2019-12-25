package cn.icuter.jsql.builder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author edward
 * @since 2018-09-12
 */
public class SQLStringBuilder {

    private String delimiter;
    private List<SQLItem> sqlItems = new LinkedList<SQLItem>();

    SQLStringBuilder(String delimiter) {
        this.setDelimiter(delimiter);
    }

    SQLStringBuilder() {
        this(" ");
    }

    /**
     * insert sql fragment into tail
     *
     * @param sqlFragment source string
     * @param fragmentType source string type
     * @return SQLStringBuilder for chain calling
     */
    public SQLStringBuilder append(String sqlFragment, String fragmentType) {
        sqlItems.add(newSQLItem(sqlFragment, fragmentType));
        return this;
    }

    /**
     * insert sql fragment into header
     *
     * @param sqlFragment source string
     * @param fragmentType source string type
     * @return SQLStringBuilder for chain calling
     */
    public SQLStringBuilder prepend(String sqlFragment, String fragmentType) {
        sqlItems.add(0, newSQLItem(sqlFragment, fragmentType));
        return this;
    }

    /**
     * insert sql fragment into any position
     *
     * @param position the position of List
     * @param sqlFragment source string
     * @param fragmentType source string type
     * @return SQLStringBuilder for chain calling
     */
    public SQLStringBuilder insert(int position, String sqlFragment, String fragmentType) {
        sqlItems.add(position, newSQLItem(sqlFragment, fragmentType));
        return this;
    }

    public SQLStringBuilder append(String sqlFragment) {
        return append(sqlFragment, null);
    }

    public SQLStringBuilder prepend(String sqlFragment) {
        return prepend(sqlFragment, null);
    }

    public SQLStringBuilder insert(int position, String sqlFragment) {
        return insert(position, sqlFragment, null);
    }

    private SQLItem newSQLItem(String sqlFragment, String sqlFragmentType) {
        SQLItem sqlItem = new SQLItem();
        sqlItem.sql = sqlFragment;
        sqlItem.type = sqlFragmentType;
        sqlItem.sqlPosition = -1;

        return sqlItem;
    }

    public int size() {
        return sqlItems.size();
    }

    public List<SQLItem> findByType(final String fragmentType) {
        return filterSQLItems(new Predicate<SQLItem>() {
            @Override
            public boolean test(SQLItem item) {
                return fragmentType == null ? item.type == null : fragmentType.equalsIgnoreCase(item.type);
            }
        });
    }

    public boolean existsType(final String fragmentType) {
        SQLItem sqlItem = findFirstSQLItem(new Predicate<SQLItem>() {
            @Override
            public boolean test(SQLItem item) {
                return fragmentType == null ? item.type == null : fragmentType.equalsIgnoreCase(item.type);
            }
        });
        return sqlItem != null;
    }

    public List<SQLItem> findBySQL(final String sql) {
        return filterSQLItems(new Predicate<SQLItem>() {
            @Override
            public boolean test(SQLItem item) {
                return sql == null ? item.sql == null : sql.equalsIgnoreCase(item.sql);
            }
        });
    }

    public SQLStringBuilder replaceByType(String fragmentType, String replacement) {
        List<SQLItem> sqlItems = findByType(fragmentType);
        for (SQLItem sqlItem : sqlItems) {
            sqlItem.sql = replacement;
        }
        return this;
    }

    /**
     * Try to find pattern that matching sql fragment
     *
     * @param pattern Regex pattern string, case insensitive
     * @return Matched SQLItem list
     */
    public List<SQLItem> findByRegex(String pattern) {
        final Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return filterSQLItems(new Predicate<SQLItem>() {
            @Override
            public boolean test(SQLItem item) {
                return p.matcher(item.sql).find();
            }
        });
    }

    private List<SQLItem> filterSQLItems(Predicate<SQLItem> predicate) {
        List<SQLItem> resultList = new ArrayList<SQLItem>();
        for (int i = 0; i < sqlItems.size(); i++) {
            SQLItem item = sqlItems.get(i);
            if (predicate.test(item)) {
                item.sqlPosition = i;
                resultList.add(item);
            }
        }
        return resultList;
    }

    private SQLItem findFirstSQLItem(Predicate<SQLItem> predicate) {
        for (int i = 0; i < sqlItems.size(); i++) {
            SQLItem item = sqlItems.get(i);
            if (predicate.test(item)) {
                item.sqlPosition = i;
                return item;
            }
        }
        return null;
    }

    public String serialize() {
        StringBuilder builder  = new StringBuilder();
        for (int i = 0; i < sqlItems.size(); i++) {
            builder.append(sqlItems.get(i).sql);
            if (i != sqlItems.size() - 1) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public static class SQLItem {
        String sql;
        String type;
        int sqlPosition;

        public String getSql() {
            return sql;
        }

        public String getType() {
            return type;
        }

        public int getSqlPosition() {
            return sqlPosition;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SQLItem{");
            sb.append("sql='").append(sql).append('\'');
            sb.append(", type='").append(type).append('\'');
            sb.append(", sqlPosition=").append(sqlPosition);
            sb.append('}');
            return sb.toString();
        }
    }
    public interface Predicate<T> {
        boolean test(T obj);
    }
}
