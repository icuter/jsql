package cn.icuter.jsql;

/**
 * @author edward
 * @since 2018-09-29
 */
public class TestTable {
    @ColumnName("test_id")
    private String testId;
    @ColumnName("t_col_1")
    private String col1;
    @ColumnName("t_col_2")
    private String col2;
    @ColumnName("order_num")
    private int orderNum;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TestTable{");
        sb.append("testId='").append(testId).append('\'');
        sb.append(", col1='").append(col1).append('\'');
        sb.append(", col2='").append(col2).append('\'');
        sb.append(", orderNum='").append(orderNum).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getCol1() {
        return col1;
    }

    public void setCol1(String col1) {
        this.col1 = col1;
    }

    public String getCol2() {
        return col2;
    }

    public void setCol2(String col2) {
        this.col2 = col2;
    }

    public int getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }
}