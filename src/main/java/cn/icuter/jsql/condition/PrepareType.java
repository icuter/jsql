package cn.icuter.jsql.condition;

/**
 * @author edward
 * @since 2018-08-09
 */
public enum PrepareType {

    IGNORE(100),
    PLAIN(101),
    PLACEHOLDER(102);

    private int type;
    PrepareType(int type) {
        this.type = type;
    }
    public int getType() {
        return type;
    }
}
