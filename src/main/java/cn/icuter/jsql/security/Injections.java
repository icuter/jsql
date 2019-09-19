package cn.icuter.jsql.security;

public abstract class Injections {

    public static void setBlacklistPattern(String[] blacklistPattern) {
        InjectionWords.getInstance().resetWords(blacklistPattern);
    }

    public static void addBlacklistPattern(String[] blacklistPattern) {
        InjectionWords.getInstance().addWords(blacklistPattern);
    }

    public static void check(String[] fields, String quoteString) {
        if (fields != null && fields.length > 0) {
            for (String field : fields) {
                check(field, quoteString);
            }
        }
    }

    /**
     * <pre>
     * Check as following field patterns
     *   1. t.col as alias => "t"."col" as "alias"
     *   2. t.col alias    => "t"."col"    "alias"
     *   3. col as alias   =>     "col" as "alias"
     *   4. col alias      =>     "col"    "alias"
     *   5. t.*            => Unsupported
     *   6. *              => Unsupported
     * </pre>
     * @param field columns in sql
     * @param quoteString valid quote string
     * @throws IllegalArgumentException if columns with black list pattern
     */
    public static void check(String field, String quoteString) {
        if (field == null) {
            return;
        }
        String tableName = null;
        int refIdx = field.indexOf('.');
        if (refIdx > 0) {
            tableName = field.substring(0, refIdx);
            field = field.substring(refIdx + 1);
        }
        if (isNotQuoted(tableName, quoteString)) {
            validateIsInBlackList(tableName);
        }
        field = field.toLowerCase();
        String colName = field;
        String colAlias = null;
        int asIdx = field.lastIndexOf(" as ");
        if (asIdx > 0) {
            colName = field.substring(0, asIdx).trim();
            colAlias = field.substring(" as ".length() + asIdx).trim();
        } else if (field.contains(" ")) {
            int spIdx = field.lastIndexOf(" ");
            colName = field.substring(0, spIdx).trim();
            colAlias = field.substring(spIdx + 1).trim();
        }
        if (isNotQuoted(colName, quoteString)) {
            validateIsInBlackList(colName);
        }
        if (isNotQuoted(colAlias, quoteString)) {
            validateIsInBlackList(colAlias);
        }
    }

    private static boolean isNotQuoted(String field, String quoteString) {
        if (field == null || field.length() <= 0) {
            return false;
        }
        if (quoteString == null || quoteString.length() <= 0) {
            return true;
        }
        return field.indexOf(quoteString) != 0 || field.lastIndexOf(quoteString) != field.length() - 1;
    }

    private static void validateIsInBlackList(String field) {
        if (field == null || field.length() <= 0) {
            return;
        }
        if (InjectionWords.getInstance().detect(field)) {
            throw new IllegalArgumentException("insecure field: " + field);
        }
    }
}
