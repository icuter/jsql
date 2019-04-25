package cn.icuter.jsql.builder;

import cn.icuter.jsql.TestTable;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.condition.Condition;
import cn.icuter.jsql.condition.Eq;
import cn.icuter.jsql.dialect.Dialects;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author edward
 * @since 2018-08-08
 */
public class BuilderTest {

    @Test
    public void testSelectBuilder() throws Exception {
        Builder select = new SelectBuilder().select().distinct()
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
        String expectedSQL = "select distinct * from t_table1 left join t_table2 on (t_table1.id=t_table2.id) " +
                "where (name = ? or age = ? or tall = ?) and (id = ? and name like ?) and birth = ? or post = ? " +
                "and user in (?,?,?) for update";
        Object[] values = new Object[]{"Edward", 30, 170, "123", "%Lee", "198910", "511442", "0000", "0001", "0002"};

        assertEquals(expectedSQL, select.getSql());
        assertArrayEquals(values, select.getPreparedValues().toArray());

        List<Condition> orConditionList = new LinkedList<>();
        orConditionList.add(Cond.eq("name", "Edward"));
        orConditionList.add(Cond.eq("age", 30));
        orConditionList.add(Cond.eq("tall", 170));

        List<Condition> andConditionList = new LinkedList<>();
        andConditionList.add(Cond.eq("id", "123"));
        andConditionList.add(Cond.like("name", "%Lee"));

        select = new SelectBuilder().select().distinct()
                .from("t_table1").leftJoinOn("t_table2", Cond.var("t_table1.id", "t_table2.id"))
                .where()
                .or(orConditionList)
                .and()
                .and(andConditionList)
                .and(Cond.eq("birth", "198910"))
                .or(Cond.eq("post", "511442"))
                .and()
                .in("user", "0000", "0001", "0002")
                .forUpdate()
                .build();
        assertEquals(expectedSQL, select.getSql());
        assertArrayEquals(values, select.getPreparedValues().toArray());

        Builder existsSelect = new SelectBuilder().select("1")
                .from("t_table1")
                .where()
                .var("t_table.id", "t_table1.id")
                .and().eq("t_table1.name", "Edward")
                .build();
        Builder notExistsSelect = new SelectBuilder().select("1")
                .from("t_table2")
                .where()
                .var("t_table.id", "t_table2.id")
                .and().ne("t_table2.name", "Apple")
                .build();
        Builder select2 = new SelectBuilder().select().from("t_table")
                .where()
                .exists(existsSelect)
                .and()
                .notExists(notExistsSelect).build();

        assertEquals("select * from t_table where " +
                "exists (select 1 from t_table1 where t_table.id=t_table1.id and t_table1.name = ?)" +
                " and not exists (select 1 from t_table2 where t_table.id=t_table2.id and t_table2.name <> ?)", select2.getSql());
        assertArrayEquals(new Object[]{"Edward", "Apple"}, select2.getPreparedValues().toArray());

        Builder builder = new SelectBuilder().select("name", "age")
                .from("t_table")
                .groupBy("name", "age").having(Cond.gt("age", 18))
                .build();
        assertEquals("select name, age from t_table group by name,age having (age > ?)", builder.getSql());
        assertArrayEquals(new Object[]{18}, builder.getPreparedValues().toArray());

        builder = new SelectBuilder().select("name", "age")
                .from("t_table")
                .orderBy("name desc", "age")
                .build();
        assertEquals("select name, age from t_table order by name desc,age", builder.getSql());

        builder = new SelectBuilder().select("name", "age")
                .from("t_table")
                .where()
                .isNull("name")
                .and()
                .isNotNull("age")
                .build();
        assertEquals("select name, age from t_table where name is null and age is not null", builder.getSql());


        builder = new SelectBuilder().select("name", "age")
                .from("t_table")
                .where()
                .and(Cond.like("name", "%Lee"), Cond.gt("age", 18))
                .and()
                .or(Cond.eq("age", 12), Cond.eq("age", 16))
                .build();
        assertEquals("select name, age from t_table where (name like ? and age > ?) and (age = ? or age = ?)", builder.getSql());
        assertArrayEquals(new Object[]{"%Lee", 18, 12, 16}, builder.getPreparedValues().toArray());
    }

    @Test
    public void testBuilderJoinOn() throws Exception {
        Builder builder = new SelectBuilder() {{
            select().from("table t")
                    .joinOn("table1 t1", Cond.var("t.id", "t1.id"), Cond.eq("t.framework", "jsql"))
                    .joinOn("table2 t2", Cond.var("t1.id", "t2.id"))
                    .build();
        }};
        assertEquals("select * from table t join table1 t1 on (t.id=t1.id and t.framework = ?) join table2 t2 on (t1.id=t2.id)", builder.getSql());
        assertArrayEquals(new Object[]{"jsql"}, builder.getPreparedValues().toArray());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .leftJoinOn("table1 t1", Cond.var("t.id", "t1.id"))
                    .leftJoinOn("table2 t2", Cond.var("t1.id", "t2.id"))
                    .build();
        }};
        assertEquals("select * from table t left join table1 t1 on (t.id=t1.id) left join table2 t2 on (t1.id=t2.id)", builder.getSql());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .rightJoinOn("table1 t1", Cond.var("t.id", "t1.id"))
                    .rightJoinOn("table2 t2", Cond.var("t1.id", "t2.id"))
                    .build();
        }};
        assertEquals("select * from table t right join table1 t1 on (t.id=t1.id) right join table2 t2 on (t1.id=t2.id)", builder.getSql());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .fullJoinOn("table1 t1", Cond.var("t.id", "t1.id"))
                    .fullJoinOn("table2 t2", Cond.var("t1.id", "t2.id"))
                    .build();
        }};
        assertEquals("select * from table t full join table1 t1 on (t.id=t1.id) full join table2 t2 on (t1.id=t2.id)", builder.getSql());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .fullJoinOn("table1 t1",
                            Cond.var("t.id", "t1.id"),
                            Cond.and(Cond.eq("t.group", "java"), Cond.eq("t.framework", "jsql"))
                    )
                    .fullJoinOn("table2 t2", Cond.var("t1.id", "t2.id"))
                    .build();
        }};
        assertEquals("select * from table t full join table1 t1 on" +
                " (t.id=t1.id and (t.group = ? and t.framework = ?)) full join table2 t2 on (t1.id=t2.id)", builder.getSql());
        assertArrayEquals(new Object[]{"java", "jsql"}, builder.getPreparedValues().toArray());
    }

    @Test
    public void testBuilderJoinUsing() throws Exception {
        Builder builder = new SelectBuilder() {{
            select().from("table t")
                    .joinUsing("table1 t1", "id")
                    .joinUsing("table2 t2", "id")
                    .build();
        }};
        assertEquals("select * from table t join table1 t1 using ( id ) join table2 t2 using ( id )", builder.getSql());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .leftJoinUsing("table1 t1", "id")
                    .leftJoinUsing("table2 t2", "id")
                    .build();
        }};
        assertEquals("select * from table t left join table1 t1 using ( id ) left join table2 t2 using ( id )", builder.getSql());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .rightJoinUsing("table1 t1", "id")
                    .rightJoinUsing("table2 t2", "id")
                    .build();
        }};
        assertEquals("select * from table t right join table1 t1 using ( id ) right join table2 t2 using ( id )", builder.getSql());

        builder = new SelectBuilder() {{
            select().from("table t")
                    .fullJoinUsing("table1 t1", "id")
                    .fullJoinUsing("table2 t2", "id")
                    .build();
        }};
        assertEquals("select * from table t full join table1 t1 using ( id ) full join table2 t2 using ( id )", builder.getSql());
    }

    @Test
    public void testSelectBuilderOffsetLimitMySQL() throws Exception {
        Builder select = new SelectBuilder(Dialects.MYSQL)
                .select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? limit ?,?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.MYSQL)
                .select().from("table").where().eq("id", "0123456789").limit(10).build();
        assertEquals("select * from table where id = ? limit ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.MYSQL)
                .select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        assertEquals("select * from table where id = ? limit ? for update", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.MYSQL)
                .select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        assertEquals("select * from table where id = ? limit ?,? for update", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());
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
        Builder select = new SelectBuilder(Dialects.DB2)
                .select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        assertEquals("select * from (select sub2_.*, rownumber() over(order by order of sub2_) as rownumber_0_ from (" +
                " select * from table where id = ? fetch first 15 rows only) as sub2_) as db2_inner1_ where rownumber_0_ > 5" +
                " order by rownumber_0_", select.getSql());
        assertArrayEquals(new Object[]{"0123456789"}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.DB2)
                .select().from("table").where().eq("id", "0123456789").limit(10).build();
        assertEquals("select * from table where id = ? fetch first 10 rows only", select.getSql());
        assertArrayEquals(new Object[]{"0123456789"}, select.getPreparedValues().toArray());
    }

    @Test
    public void testSelectBuilderOffsetLimitOracle() throws Exception {
        Builder select = new SelectBuilder(Dialects.ORACLE)
                .select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        assertEquals("select * from (select source_.*, rownum rownumber_0_ from ( " +
                "select * from table where id = ? ) source_ where rownum <= ?) where rownumber_0_ > ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 15, 5}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.ORACLE)
                .select().from("table").where().eq("id", "0123456789").limit(10).build();
        assertEquals("select * from (" +
                " select * from table where id = ? ) where rownum <= ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.ORACLE)
                .select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        assertEquals("select * from (select source_.*, rownum rownumber_0_ from (" +
                " select * from table where id = ? for update ) source_ where rownum <= ?) where rownumber_0_ > ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 15, 5}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.ORACLE)
                .select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        assertEquals("select * from ( select * from table where id = ? for update ) where rownum <= ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());
    }

    @Test
    public void testSelectBuilderOffsetLimitDerby() throws Exception {
        Builder select = new SelectBuilder(Dialects.EMBEDDED_DERBY)
                .select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? offset ? rows fetch next ? rows only", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.EMBEDDED_DERBY)
                .select().from("table").where().eq("id", "0123456789").limit(10).build();
        assertEquals("select * from table where id = ? fetch first ? rows only", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.EMBEDDED_DERBY)
                .select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        assertEquals("select * from table where id = ? offset ? rows fetch next ? rows only for update", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.EMBEDDED_DERBY)
                .select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        assertEquals("select * from table where id = ? fetch first ? rows only for update", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.EMBEDDED_DERBY)
                .select().from("table").where().eq("id", "0123456789").forUpdate().sql("WITH RS").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? offset ? rows fetch next ? rows only for update WITH RS", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.EMBEDDED_DERBY)
                .select().from("table").where().eq("id", "0123456789").sql("WITH RS").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? offset ? rows fetch next ? rows only WITH RS", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());
    }

    @Test
    public void testSelectBuilderOffsetLimitH2() throws Exception {
        Builder select = new SelectBuilder(Dialects.H2)
                .select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? limit ? offset ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10, 5}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.H2)
                .select().from("table").where().eq("id", "0123456789").limit(10).build();
        assertEquals("select * from table where id = ? limit ?", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.H2)
                .select().from("table").where().eq("id", "0123456789").forUpdate().offset(5).limit(10).build();
        assertEquals("select * from table where id = ? limit ? offset ? for update", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10, 5}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.H2)
                .select().from("table").where().eq("id", "0123456789").forUpdate().limit(10).build();
        assertEquals("select * from table where id = ? limit ? for update", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());
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
        Builder select = new SelectBuilder(Dialects.SQLSERVER2012_PLUS)
                .select().from("table").where().eq("id", "0123456789").orderBy("id desc").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? order by id desc offset ? rows fetch next ? rows only", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());

        select = new SelectBuilder(Dialects.SQLSERVER2012_PLUS)
                .select().from("table").where().eq("id", "0123456789").orderBy("id desc").limit(10).build();
        assertEquals("select * from table where id = ? order by id desc offset 0 rows fetch next ? rows only", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 10}, select.getPreparedValues().toArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectBuilderOffsetLimitSQLServer2012Exception() throws Exception {
        Builder select = new SelectBuilder(Dialects.SQLSERVER2012_PLUS)
                .select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
        assertEquals("select * from table where id = ? offset ? rows fetch next ? rows only", select.getSql());
        assertArrayEquals(new Object[]{"0123456789", 5, 10}, select.getPreparedValues().toArray());
    }

    @Test
    public void testSelectWithInBuilder() throws Exception {
        Builder selectCondIn = new SelectBuilder()
                .select("name").from("table_1").where().like("name", "%Edward%").build();
        Builder select = new SelectBuilder() {{
            select().from("table").where().in("name", selectCondIn).build();
        }};
        assertEquals("select * from table where name in (select name from table_1 where name like ?)", select.getSql());
        assertArrayEquals(new Object[]{"%Edward%"}, select.getPreparedValues().toArray());
    }

    @Test
    public void testSelectUnion() {
        Builder select = UnionSelectBuilder.union(
                new SelectBuilder().select("t_id as id", "t_name as name").from("table").where().eq("region", "Canton").build(),
                new SelectBuilder().select("id", "name").from("table_1").where().eq("region", "China").build()
        ).build();
        assertEquals("select * from ( select * from (select t_id as id, t_name as name from table where region = ?) t " +
                "union select * from (select id, name from table_1 where region = ?) t ) union_alias_", select.getSql());
        assertArrayEquals(new Object[]{"Canton", "China"}, select.getPreparedValues().toArray());

        select = UnionSelectBuilder.unionAll(
                new SelectBuilder().select("t_id as id", "t_name as name").from("table").where().eq("region", "Canton").build(),
                new SelectBuilder().select("id", "name").from("table_1").where().eq("region", "China").build()
        ).build();
        assertEquals("select * from ( select * from (select t_id as id, t_name as name from table where region = ?) t " +
                "union all select * from (select id, name from table_1 where region = ?) t ) union_alias_", select.getSql());
        assertArrayEquals(new Object[]{"Canton", "China"}, select.getPreparedValues().toArray());

        Builder oracleUnionBuilder = new UnionSelectBuilder(Dialects.ORACLE);
        Builder select1 = new SelectBuilder(Dialects.ORACLE);
        select1.select("col1", "col2", "col3").from("t_table").offset(20).limit(10).build();
        Builder select2 = new SelectBuilder(Dialects.ORACLE);
        select2.select("col1", "col2", "col3").from("t_table").build();
        oracleUnionBuilder.union(select1).union(select2).offset(10).limit(100).build();
        assertEquals("select * from (select source_.*, rownum rownumber_2_ from" +
                " ( select * from ( select * from (select * from (select source_.*, rownum rownumber_0_ from" +
                " ( select col1, col2, col3 from t_table ) source_ where rownum <= ?) where rownumber_0_ > ?) t" +
                " union" +
                " select * from (select oracle_alias_.*, rownum as rownumber_0_ from (select col1, col2, col3 from t_table) oracle_alias_) t ) union_alias_ )" +
                " source_ where rownum <= ?) where rownumber_2_ > ?", oracleUnionBuilder.getSql());
        assertArrayEquals(new Object[] {30, 20, 110, 10}, oracleUnionBuilder.getPreparedValues().toArray());

        Builder db2UnionBuilder = UnionSelectBuilder.union(Dialects.DB2,
                new SelectBuilder(Dialects.DB2).select("t_id as id", "t_name as name").from("table")
                        .where().eq("region", "Canton").offset(10).limit(20).build(),
                new SelectBuilder().select("id", "name").from("table_1").where().eq("region", "China").build()
        ).offset(10).limit(20).build();
        assertEquals("select * from" +
                " (select sub2_.*, rownumber() over(order by order of sub2_) as rownumber_2_ from" +
                " ( select * from ( select * from (select * from (select sub2_.*, rownumber() over(order by order of sub2_) as rownumber_0_" +
                " from ( select t_id as id, t_name as name from table where region = ? fetch first 30 rows only) as sub2_)" +
                " as db2_inner1_ where rownumber_0_ > 10 order by rownumber_0_) t" +
                " union select * from (select db2_alias_.*, rownumber() OVER (ORDER BY order of db2_alias_) AS rownumber_0_ from (select id, name from table_1 where region = ?) as db2_alias_) t )" +
                " fetch first 30 rows only) as sub2_) as db2_inner1_ where rownumber_2_ > 10 order by rownumber_2_", db2UnionBuilder.getSql());
        assertArrayEquals(new Object[] {"Canton", "China"}, db2UnionBuilder.getPreparedValues().toArray());

        Builder mysqlUnionBuilder = UnionSelectBuilder.union(Dialects.MYSQL,
                new SelectBuilder(Dialects.MYSQL).select("t_id as id", "t_name as name").from("table")
                        .where().eq("region", "Canton").offset(10).limit(20).build(),
                new SelectBuilder().select("id", "name").from("table_1").where().eq("region", "China").build()
        ).build();
        assertEquals("select * from ( select * from" +
                " (select t_id as id, t_name as name from table where region = ? limit ?,?) t" +
                " union" +
                " select * from (select id, name from table_1 where region = ?) t ) union_alias_", mysqlUnionBuilder.getSql());
        assertArrayEquals(new Object[] {"Canton", 10, 20, "China"}, mysqlUnionBuilder.getPreparedValues().toArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnionException() {
        Builder select1 = new SelectBuilder(Dialects.ORACLE).select().from("t_table").where().eq("id", 1).offset(1).limit(10).build();
        Builder select2 = new SelectBuilder(Dialects.DB2).select().from("t_table").where().eq("id", 1).offset(1).limit(10).build();

        Builder unionBuilder = new UnionSelectBuilder(Dialects.ORACLE);
        unionBuilder.union(select1).union(select2).build();
    }

    @Test
    public void testBuilderValue() throws Exception {
        Builder selectMaxAge = new SelectBuilder();
        selectMaxAge.select("max(age)").from("t_person").where().eq("class", "net engine 01").build();
        Builder select = new SelectBuilder();
        select.select("id", "age", "name", "class").from("t_person").where().like("name", "Lee%").and().eq("age", selectMaxAge).build();
        assertEquals("select id, age, name, class from t_person where name like ? and age = (select max(age) from t_person where class = ?)",
                select.getSql());
        assertArrayEquals(new Object[] {"Lee%", "net engine 01"}, select.getPreparedValues().toArray());
    }

    @Test
    public void testUpdateBuilder() throws Exception {
        Builder update = new UpdateBuilder()
                .update("t_table")
                .set(
                        Cond.eq("name", "Edward"),
                        Cond.eq("age", 30),
                        Cond.eq("tall", 170)
                )
                .where()
                .like("id", "123%")
                .build();
        assertEquals("update t_table set name = ?,age = ?,tall = ? where id like ?", update.getSql());
        assertArrayEquals(new Object[]{"Edward", 30, 170, "123%"}, update.getPreparedValues().toArray());
    }

    @Test
    public void testInsertBuilder() throws Exception {
        Builder insert = new InsertBuilder()
                .insert("t_table")
                .values(
                        Cond.eq("col1", "Edward"),
                        Cond.eq("col2", 170),
                        Cond.eq("col3", "1989-02-01")
                ).build();
        assertEquals("insert into t_table (col1,col2,col3) values(?,?,?)", insert.getSql());
        assertArrayEquals(new Object[]{"Edward", 170, "1989-02-01"}, insert.getPreparedValues().toArray());

        TestTable testTable = new TestTable();
        testTable.setTestId("test01");
        testTable.setCol1("test00");
        testTable.setCol2("123");
        insert = new InsertBuilder().insert("t_jsql_test").values(testTable).build();
        assertEquals("insert into t_jsql_test (test_id,t_col_1,t_col_2,order_num) values(?,?,?,?)", insert.getSql());
        assertArrayEquals(new Object[]{"test01", "test00", "123", 0}, insert.getPreparedValues().toArray());

        insert = new InsertBuilder()
                .insert("t_jsql_test", "col1")
                .values(Arrays.asList("col1_value"))
                .build();
        assertEquals("insert into t_jsql_test (col1) values(?)", insert.getSql());
        assertArrayEquals(new Object[]{"col1_value"}, insert.getPreparedValues().toArray());

        insert = new InsertBuilder()
                .insert("t_jsql_test", "col1", "col2")
                .values(Arrays.asList("col1_value", "col2_value"))
                .build();
        assertEquals("insert into t_jsql_test (col1,col2) values(?,?)", insert.getSql());
        assertArrayEquals(new Object[]{"col1_value", "col2_value"}, insert.getPreparedValues().toArray());

        insert = new InsertBuilder()
                .insert("t_jsql_test", "col1", "col2")
                .values(new Object[] {"col1_value", "col2_value"})
                .build();
        assertEquals("insert into t_jsql_test (col1,col2) values(?,?)", insert.getSql());
        assertArrayEquals(new Object[]{"col1_value", "col2_value"}, insert.getPreparedValues().toArray());

        insert = new InsertBuilder()
                .insert("t_jsql_test")
                .values(Cond.eq("col1", "col1_value"), Cond.eq("col2", "col2_value"))
                .build();
        assertEquals("insert into t_jsql_test (col1,col2) values(?,?)", insert.getSql());
        assertArrayEquals(new Object[]{"col1_value", "col2_value"}, insert.getPreparedValues().toArray());

        List<Eq> list = new LinkedList<>();
        list.add(Cond.eq("col1", "col1_value"));
        list.add(Cond.eq("col2", "col2_value"));
        insert = new InsertBuilder()
                .insert("t_jsql_test")
                .values(list)
                .build();
        assertEquals("insert into t_jsql_test (col1,col2) values(?,?)", insert.getSql());
        assertArrayEquals(new Object[]{"col1_value", "col2_value"}, insert.getPreparedValues().toArray());

        insert = new InsertBuilder()
                .insert("t_jsql_test", "col1", "col2")
                .select("col1", "col2").from("t_jsql_test_2").where().eq("name", "iCuter")
                .build();
        assertEquals("insert into t_jsql_test (col1,col2) select col1, col2 from t_jsql_test_2 where name = ?", insert.getSql());
        assertArrayEquals(new Object[]{"iCuter"}, insert.getPreparedValues().toArray());


    }

    @Test
    public void testBuilderSql() {
        Builder builder = new SelectBuilder().sql("select 1 from table where id = ?").value(123456789).build();
        assertEquals("select 1 from table where id = ?", builder.getSql());
        assertArrayEquals(new Object[]{ 123456789 }, builder.getPreparedValues().toArray());
    }

    @Test
    public void testDeleteBuilder() throws Exception {
        Builder delete = new DeleteBuilder().delete().from("t_table").where().eq("id", 123456789).build();
        assertEquals("delete from t_table where id = ?", delete.getSql());
        assertArrayEquals(new Object[]{ 123456789 }, delete.getPreparedValues().toArray());
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