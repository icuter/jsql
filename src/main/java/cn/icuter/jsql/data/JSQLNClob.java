package cn.icuter.jsql.data;

/**
 * @author edward
 * @since 2019-02-07
 */
public class JSQLNClob extends JSQLClob implements java.sql.NClob {

    public JSQLNClob() {
    }

    public JSQLNClob(String initData) {
        super(initData);
    }
}
