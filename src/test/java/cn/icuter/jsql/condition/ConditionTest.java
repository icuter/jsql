package cn.icuter.jsql.condition;

import static org.junit.Assert.*;

/**
 * @author edward
 * @since 2018-08-08
 */
public class ConditionTest {

    @org.junit.Test
    public void toSql() throws Exception {
        assertEquals(Cond.eq("name", "").toSql(), "name = ?");
        assertEquals(Cond.ne("name", "").toSql(), "name <> ?");
        assertEquals(Cond.lt("name", "").toSql(), "name < ?");
        assertEquals(Cond.le("name", "").toSql(), "name <= ?");
        assertEquals(Cond.gt("name", "").toSql(), "name > ?");
        assertEquals(Cond.ge("name", "").toSql(), "name >= ?");
        assertEquals(Cond.like("name", "").toSql(), "name like ?");
        assertEquals(Cond.notLike("name", "").toSql(), "name not like ?");
        assertEquals(Cond.isNull("name").toSql(), "name is null");
        assertEquals(Cond.eq("name", null).toSql(), "name = ?");
        assertEquals(Cond.isNotNull("name").toSql(), "name is not null");
        assertEquals(Cond.ne("name", null).toSql(), "name <> ?");
        assertEquals(Cond.between("tall", 160, 170).toSql(), "tall between ? and ?");
        assertEquals(Cond.in("post", 511442, 510412).toSql(), "post in (?,?)");

        assertEquals(Cond.and(Cond.eq("name", "")).toSql(), "(name = ?)");
        assertEquals(Cond.and(Cond.eq("name", ""), Cond.eq("age", 0)).toSql(), "(name = ? and age = ?)");
        assertEquals(Cond.or(Cond.eq("name", "")).toSql(), "(name = ?)");
        assertEquals(Cond.or(Cond.eq("name", ""), Cond.eq("age", 0)).toSql(), "(name = ? or age = ?)");
    }

}