package cn.icuter.jsql.security;

public abstract class Injections {

    private static volatile String[] blacklistPattern = {";", "--", "/*", "#", "%", "?", "@", "'", "\"", "(", ")"};

    public static void setBlacklistPattern(String[] blacklistPattern) {
        Injections.blacklistPattern = blacklistPattern;
    }

    public static void addBlacklistPattern(String[] blacklistPattern) {
        String[] newPatternArr = new String[blacklistPattern.length + Injections.blacklistPattern.length];
        for (int i = 0; i < newPatternArr.length; i++) {
            if (i < Injections.blacklistPattern.length) {
                newPatternArr[i] = Injections.blacklistPattern[i];
            } else {
                newPatternArr[i] = blacklistPattern[i - Injections.blacklistPattern.length];
            }
        }
        Injections.blacklistPattern = newPatternArr;
    }

    public static void check(String[] fields, String quoteString) {
        if (fields != null && fields.length > 0) {
            for (String field : fields) {
                check(field, quoteString);
            }
        }
    }

    public static void check(String field, String quoteString) {
        if (field == null) {
            return;
        }
        if (isQuoted(field, quoteString)) {
            return;
        }
        field = field.toLowerCase();
        String colName = field;
        String colAlias = null;
        int asIdx = field.indexOf(" as ");
        if (asIdx > 0) {
            // col as alias
            colName = field.substring(0, asIdx).trim();
            colAlias = field.substring(" as ".length() + asIdx).trim();
        } else if (field.contains(" ")) {
            // col alias
            int spIdx = field.indexOf(" ");
            colName = field.substring(0, spIdx).trim();
            colAlias = field.substring(spIdx + 1).trim();
        }
        if (colName.length() > 0) {
            if (quoteString != null && quoteString.length() > 0 && isQuoted(colName, quoteString)) {
                return;
            }
            validateIsInBlackList(colName);
        }
        if (colAlias != null) {
            if (quoteString != null && quoteString.length() > 0 && isQuoted(colAlias, quoteString)) {
                return;
            }
            validateIsInBlackList(colAlias);
        }
    }

    private static boolean isQuoted(String field, String quoteString) {
        if (field == null || field.length() <= 0) {
            return true;
        }
        return field.indexOf(quoteString) == 0 && field.lastIndexOf(quoteString) == field.length() - 1;
    }

    private static void validateIsInBlackList(String field) {
        if (field == null || field.length() <= 0) {
            return;
        }
        for (String blacklistStr : blacklistPattern) {
            int idx = field.indexOf(blacklistStr);
            if (idx >= 0) {
                throw new IllegalArgumentException("insecure field: " + field);
            }
        }
    }
}
