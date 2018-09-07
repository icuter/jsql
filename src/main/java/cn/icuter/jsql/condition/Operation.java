package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-05
 */
public enum Operation {

    NOOP(""),
    NULL("is null"),
    NOT_NULL("is not null"),
    BETWEEN("between"),
    LIKE("like"),
    NOT_LIKE("not like"),
    NE("<>"),
    EQ("="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    IN("in");

    private String symbol;
    Operation(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
