package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-05
 */
public enum Combination {

    AND("and"),
    OR("or");

    private String symbol;
    Combination(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
