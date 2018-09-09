package cn.icuter.jsql.column;

import cn.icuter.jsql.ColumnName;

/**
 * @author edward
 * @since 2018-08-24
 */
public class OrgUnit {

    @ColumnName("org_id")
    private String orgId;

    @ColumnName("org_unit_id")
    private String ouId;

    @ColumnName("parent_org_unit_id")
    private String pOuId;

    @ColumnName("org_unit_name")
    private String ouName;

    @ColumnName("org_unit_list_rank")
    private Integer ouListRank;

    @Override
    public String toString() {
        return "OrgUnit{" +
                "orgId='" + orgId + '\''
                + ", ouId='" + ouId + '\''
                + ", pOuId='" + pOuId + '\''
                + ", ouName='" + ouName + '\''
                + ", ouListRank=" + ouListRank
                + '}';
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOuId() {
        return ouId;
    }

    public void setOuId(String ouId) {
        this.ouId = ouId;
    }

    public String getpOuId() {
        return pOuId;
    }

    public void setpOuId(String pOuId) {
        this.pOuId = pOuId;
    }

    public String getOuName() {
        return ouName;
    }

    public void setOuName(String ouName) {
        this.ouName = ouName;
    }

    public Integer getOuListRank() {
        return ouListRank;
    }

    public void setOuListRank(Integer ouListRank) {
        this.ouListRank = ouListRank;
    }
}
