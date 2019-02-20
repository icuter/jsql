package cn.icuter.jsql.data;

import java.sql.NClob;
import java.sql.SQLException;

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

    public NClob copyTo(NClob targetNclob) throws SQLException {
        targetNclob.setString(1, getSubString(1, (int) length()));
        return targetNclob;
    }

    public String getNClobString() {
        return getClobString();
    }
}
