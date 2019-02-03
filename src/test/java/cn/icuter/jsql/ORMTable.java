package cn.icuter.jsql;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;

/**
 * @author edward
 * @since 2019-02-02
 */
public class ORMTable {
    @ColumnName("orm_id")
    private String ormId;
    @ColumnName("f_blob")
    private byte[] fBlob;
    @ColumnName("f_blob")
    private Blob fBlobObj;
    @ColumnName("f_clob")
    private String fClob;
    @ColumnName("f_clob")
    private Clob fClobObj;
    @ColumnName("f_string")
    private String fString;
    @ColumnName("f_int")
    private int fInt;
    @ColumnName("f_integer")
    private Integer fInteger;
    @ColumnName("f_double")
    private double fDouble;
    @ColumnName("f_decimal")
    private BigDecimal fDecimal;

    public String getOrmId() {
        return ormId;
    }

    public void setOrmId(String ormId) {
        this.ormId = ormId;
    }

    public byte[] getfBlob() {
        return fBlob;
    }

    public void setfBlob(byte[] fBlob) {
        this.fBlob = fBlob;
    }

    public Blob getfBlobObj() {
        return fBlobObj;
    }

    public void setfBlobObj(Blob fBlobObj) {
        this.fBlobObj = fBlobObj;
    }

    public String getfClob() {
        return fClob;
    }

    public void setfClob(String fClob) {
        this.fClob = fClob;
    }

    public Clob getfClobObj() {
        return fClobObj;
    }

    public void setfClobObj(Clob fClobObj) {
        this.fClobObj = fClobObj;
    }

    public String getfString() {
        return fString;
    }

    public void setfString(String fString) {
        this.fString = fString;
    }

    public int getfInt() {
        return fInt;
    }

    public void setfInt(int fInt) {
        this.fInt = fInt;
    }

    public Integer getfInteger() {
        return fInteger;
    }

    public void setfInteger(Integer fInteger) {
        this.fInteger = fInteger;
    }

    public double getfDouble() {
        return fDouble;
    }

    public void setfDouble(double fDouble) {
        this.fDouble = fDouble;
    }

    public BigDecimal getfDecimal() {
        return fDecimal;
    }

    public void setfDecimal(BigDecimal fDecimal) {
        this.fDecimal = fDecimal;
    }
}
