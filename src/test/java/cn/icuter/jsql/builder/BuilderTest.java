package cn.icuter.jsql.builder;

import cn.icuter.jsql.column.OrgUnit;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.dialect.Dialects;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author edward
 * @since 2018-08-08
 */
public class BuilderTest {

    @Test
    public void testSelectBuilder() throws Exception {
        Builder select = new SelectBuilder() {{
            select().distinct()
                    .from("t_table1").leftJoinOn("t_table2", Cond.var("t_table1.id", "t_table2.id"))
                    .where()
                    .or(
                            Cond.eq("name", "Edward"),
                            Cond.eq("age", 30),
                            Cond.eq("tall", 170)
                    )
                    .and()
                    .and(
                            Cond.eq("id", "123"),
                            Cond.like("name", "%Lee")
                    )
                    .and(Cond.eq("birth", "198910"))
                    .or(Cond.eq("post", "511442"))
                    .and()
                    .in("user", "0000", "0001", "0002")
                    .forUpdate()
                    .build();
        }};
        assertEquals(select.getSql(), "select distinct * from t_table1 left join t_table2 on t_table1.id=t_table2.id " +
                "where ( name = ? or age = ? or tall = ?) and ( id = ? and name like ?) and birth = ? or post = ? " +
                "and user in (?,?,?) for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"Edward", 30, 170, "123", "%Lee",
                "198910", "511442", "0000", "0001", "0002"});

        Builder existsSelect = new SelectBuilder() {{
            select("1")
                    .from("t_table1")
                    .where()
                    .var("t_table.id", "t_table1.id")
                    .and().eq("t_table1.name", "Edward")
                    .build();
        }};
        Builder notExistsSelect = new SelectBuilder() {{
            select("1")
                    .from("t_table2")
                    .where()
                    .var("t_table.id", "t_table2.id")
                    .and().ne("t_table2.name", "Apple")
                    .build();
        }};
        Builder select2 = new SelectBuilder() {{
            select().from("t_table")
                    .where()
                    .exists(existsSelect)
                    .and()
                    .notExists(notExistsSelect).build();
        }};

        assertEquals(select2.getSql(), "select * from t_table where " +
                "exists (select 1 from t_table1 where t_table.id=t_table1.id and t_table1.name = ?)" +
                " and not exists (select 1 from t_table2 where t_table.id=t_table2.id and t_table2.name <> ?)"
        );
        assertArrayEquals(select2.getPreparedValues().toArray(), new Object[] {"Edward", "Apple"});
    }

    @Test
    public void testSelectBuilderOffsetLimitMySQL() throws Exception {
        Builder select = new SelectBuilder(Dialects.MYSQL) {{
            select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ?,?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 5, 10});

        select = new SelectBuilder(Dialects.MYSQL) {{
            select().from("table").where().eq("id", "0123456789").limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});

        select = new SelectBuilder(Dialects.MYSQL) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ? for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});

        select = new SelectBuilder(Dialects.MYSQL) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ?,? for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 5, 10});
    }

    @Test
    public void testSelectBuilderOffsetLimitMariaDB() throws Exception {
        // same as MYSQLDialect
    }

    /*
     * DB2 dont support both FOR UPDATE and FETCH FIRST in one sql
     */
    @Test
    public void testSelectBuilderOffsetLimitDB2() throws Exception {
        Builder select = new SelectBuilder(Dialects.DB2) {{
            select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from ( select sub2_.*, rownumber() over(order by order of sub2_) as _rownumber_ from ( " +
                "select * from table where id = ? fetch first 10 rows only ) as sub2_ ) as inner1_ where _rownumber_ > 5" +
                " order by _rownumber_");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});

        select = new SelectBuilder(Dialects.DB2) {{
            select().from("table").where().eq("id", "0123456789").limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? fetch first 10 rows only");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});
    }

    @Test
    public void testSelectBuilderOffsetLimitOracle() throws Exception {
        Builder select = new SelectBuilder(Dialects.ORACLE) {{
            select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from ( select _source.*, rownum _rownum from ( " +
                "select * from table where id = ? ) _source where rownum <= ?) where _rownum > ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10, 5});

        select = new SelectBuilder(Dialects.ORACLE) {{
            select().from("table").where().eq("id", "0123456789").limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from ( " +
                "select * from table where id = ? ) where rownum <= ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});

        select = new SelectBuilder(Dialects.ORACLE) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from ( select _source.*, rownum _rownum from ( " +
                "select * from table where id = ? ) _source where rownum <= ?) where _rownum > ? for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10, 5});

        select = new SelectBuilder(Dialects.ORACLE) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from ( " +
                "select * from table where id = ? ) where rownum <= ? for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});
    }

    @Test
    public void testSelectBuilderOffsetLimitDerby() throws Exception {
        Builder select = new SelectBuilder(Dialects.DERBY) {{
            select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? offset 5 rows fetch next 10 rows only");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});

        select = new SelectBuilder(Dialects.DERBY) {{
            select().from("table").where().eq("id", "0123456789").limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? fetch first 10 rows only");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});

        select = new SelectBuilder(Dialects.DERBY) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? offset 5 rows fetch next 10 rows only for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});

        select = new SelectBuilder(Dialects.DERBY) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? fetch first 10 rows only for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});

        select = new SelectBuilder(Dialects.DERBY) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().sql("WITH RS").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? offset 5 rows fetch next 10 rows only for update WITH RS");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789"});
    }

    @Test
    public void testSelectBuilderOffsetLimitH2() throws Exception {
        Builder select = new SelectBuilder(Dialects.H2) {{
            select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ? offset ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10, 5});

        select = new SelectBuilder(Dialects.H2) {{
            select().from("table").where().eq("id", "0123456789").limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});

        select = new SelectBuilder(Dialects.H2) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ? offset ? for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10, 5});

        select = new SelectBuilder(Dialects.H2) {{
            select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? limit ? for update");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});
    }

    @Test
    public void testSelectBuilderOffsetLimitCUBRID() throws Exception {
        // same as MYSQLDialect
    }

    @Test
    public void testSelectBuilderOffsetLimitPostgreSQL() throws Exception {
        // same as H2
    }

    @Test
    public void testSelectBuilderOffsetLimitSQLServer2012() throws Exception {
        Builder select = new SelectBuilder(Dialects.SQLSERVER2012) {{
            select().from("table").where().eq("id", "0123456789").orderBy("id desc").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? order by id desc offset ? rows fetch next ? rows only");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 5, 10});

        select = new SelectBuilder(Dialects.SQLSERVER2012) {{
            select().from("table").where().eq("id", "0123456789").orderBy("id desc").limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? order by id desc offset 0 rows fetch next ? rows only");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 10});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectBuilderOffsetLimitSQLServer2012Exception() throws Exception {
        Builder select = new SelectBuilder(Dialects.SQLSERVER2012) {{
            select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        }};
        assertEquals(select.getSql(), "select * from table where id = ? offset ? rows fetch next ? rows only");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"0123456789", 5, 10});
    }

    @Test
    public void testSelectWithInBuilder() throws Exception {
        Builder selectCondIn = new SelectBuilder() {{
            select("name").from("table_1").where().like("name", "%Edward%").build();
        }};
        Builder select = new SelectBuilder() {{
            select().from("table").where().in("name", selectCondIn).build();
        }};
        assertEquals(select.getSql(), "select * from table where name in (select name from table_1 where name like ?)");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"%Edward%"});
    }

    @Test
    public void testSelectUnion() {
        Builder select = new SelectBuilder() {{
            select("t_id as id", "t_name as name").from("table").where().eq("region", "Canton")
                    .union(new SelectBuilder() {{
                        select("id", "name").from("table_1").where().eq("region", "China").build();
                    }}).build();
        }};
        assertEquals(select.getSql(), "select t_id as id, t_name as name from table where region = ? " +
                "union select id, name from table_1 where region = ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"Canton", "China"});

        select = new SelectBuilder() {{
            select("t_id as id", "t_name as name").from("table").where().eq("region", "Canton")
                    .unionAll(new SelectBuilder() {{
                        select("id", "name").from("table_1").where().eq("region", "China").build();
                    }}).build();
        }};
        assertEquals(select.getSql(), "select t_id as id, t_name as name from table where region = ? " +
                "union all select id, name from table_1 where region = ?");
        assertArrayEquals(select.getPreparedValues().toArray(), new Object[]{"Canton", "China"});
    }

    @Test
    public void testUpdateBuilder() throws Exception {
        Builder update = new UpdateBuilder() {{
            update("t_table")
                    .set(
                            Cond.eq("name", "Edward"),
                            Cond.eq("age", 30),
                            Cond.eq("tall", 170)
                    )
                    .where()
                    .like("id", "123%")
                    .build();
        }};
        assertEquals(update.getSql(), "update t_table set name = ?, age = ?, tall = ? where id like ?");
        assertArrayEquals(update.getPreparedValues().toArray(), new Object[]{"Edward", 30, 170, "123%"});
    }

    @Test
    public void testInsertBuilder() throws Exception {
        Builder insert = new InsertBuilder() {{
            insertInto("t_table")
                    .values(
                            Cond.eq("col1", "Edward"),
                            Cond.eq("col2", 170),
                            Cond.eq("col3", "1989-02-01")
                    ).build();
        }};
        assertEquals(insert.getSql(), "insert into t_table(col1,col2,col3) values(?,?,?)");
        assertArrayEquals(insert.getPreparedValues().toArray(), new Object[]{"Edward", 170, "1989-02-01"});

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setOrgId("a");
        orgUnit.setOuId("test01");
        orgUnit.setpOuId("test00");
        orgUnit.setOuListRank(123);
        insert = new InsertBuilder() {{
            insertInto("t_org_unit").values(orgUnit).build();
        }};
        assertEquals(insert.getSql(),
                "insert into t_org_unit(org_id,org_unit_id,parent_org_unit_id,org_unit_list_rank) values(?,?,?,?)");
        assertArrayEquals(insert.getPreparedValues().toArray(), new Object[]{"a", "test01", "test00", 123});
    }

    @Test
    public void testBuilderSql() {
        Builder builder = new SelectBuilder() {{
            sql("select 1 from table where id = ?").value(123456789).build();
        }};
        assertEquals(builder.getSql(), "select 1 from table where id = ?");
        assertArrayEquals(builder.getPreparedValues().toArray(), new Object[]{ 123456789 });
    }

    @Test
    public void testDeleteBuilder() throws Exception {
        Builder delete = new DeleteBuilder() {{
            delete().from("t_table").where().eq("id", 123456789).build();
        }};
        assertEquals(delete.getSql(), "delete from t_table where id = ?");
        assertArrayEquals(delete.getPreparedValues().toArray(), new Object[]{123456789});
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSelectException() throws Exception {
        Builder select = new SelectBuilder();
        select.update("t_table");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateException() throws Exception {
        Builder update = new UpdateBuilder();
        update.forUpdate();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInsertException() throws Exception {
        Builder insert = new InsertBuilder();
        insert.delete();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteException() throws Exception {
        Builder delete = new DeleteBuilder();
        delete.set(Cond.eq("id", "test"));
    }
}