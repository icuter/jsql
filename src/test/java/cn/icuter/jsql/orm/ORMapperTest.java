package cn.icuter.jsql.orm;

import cn.icuter.jsql.ORMTable;
import cn.icuter.jsql.data.JSQLBlob;
import cn.icuter.jsql.data.JSQLClob;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Map;
import java.util.UUID;

/**
 * @author edward
 * @since 2019-02-15
 */
public class ORMapperTest {
    @Test
    public void toMap() throws Exception {
        ORMTable ormTable = createOrmTable();
        ORMapper<ORMTable> orMapper = ORMapper.of(ormTable);
        Map<String, Object> map = orMapper.toMap();
        Assert.assertEquals(ormTable.getfString(), map.get("f_string"));
        Assert.assertEquals(ormTable.getfInteger(), map.get("f_integer"));
        Assert.assertEquals(ormTable.getfInt(), map.get("f_int"));
        Assert.assertEquals(ormTable.getfDecimal(), map.get("f_decimal"));
        Assert.assertEquals(ormTable.getfDouble(), map.get("f_double"));
        Assert.assertEquals(ormTable.getfClobObj(), map.get("f_clob"));
        Blob blob = (Blob) map.get("f_blob");
        Assert.assertArrayEquals(ormTable.getfBlobObj().getBytes(1L,
                (int) ormTable.getfBlobObj().length()), blob.getBytes(1L, (int) blob.length()));
        Clob clob = (Clob) map.get("f_clob");
        Assert.assertEquals(ormTable.getfClobObj().getSubString(1L,
                (int) ormTable.getfClobObj().length()), clob.getSubString(1L, (int) clob.length()));

        ormTable.setfInteger(null);
        orMapper = ORMapper.of(ormTable);
        map = orMapper.toMap();
        Assert.assertTrue(map.containsKey("f_integer"));
        Assert.assertNull(map.get("f_integer"));

        map = orMapper.toMapIgnoreNullValue();
        Assert.assertFalse(map.containsKey("f_integer"));
        Assert.assertNull(map.get("f_integer"));

        ormTable = createOrmTable();
        orMapper = ORMapper.of(ormTable);
        map = orMapper.toMap((object, fieldName, colName, value, resultMap) -> "f_clob".equals(colName));
        Assert.assertTrue(map.containsKey("f_clob"));
        Assert.assertFalse(map.containsKey("f_int"));
        Assert.assertFalse(map.containsKey("f_string"));
        Assert.assertFalse(map.containsKey("f_integer"));
        Assert.assertFalse(map.containsKey("f_decimal"));
        Assert.assertFalse(map.containsKey("f_double"));
        Assert.assertFalse(map.containsKey("f_blob"));
        clob = (Clob) map.get("f_clob");
        Assert.assertEquals(ormTable.getfClobObj().getSubString(1L,
                (int) ormTable.getfClobObj().length()), clob.getSubString(1L, (int) clob.length()));
    }

    @Test
    public void mapColumn() throws Exception {
        // TODO
    }

    private ORMTable createOrmTable() {
        ORMTable ormTable = new ORMTable();
        ormTable.setfString("test string");
        ormTable.setfInteger(100);
        ormTable.setfInt(100);
        ormTable.setfDecimal(new BigDecimal("100.002"));
        ormTable.setfDouble(100.00d);
        ormTable.setOrmId(UUID.randomUUID().toString());
        ormTable.setfClobObj(new JSQLClob("test jsql clob"));
        ormTable.setfBlobObj(new JSQLBlob("test jsql clob".getBytes()));
        return ormTable;
    }

}