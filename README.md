JSQL
======
Welcome to JSQL world. It's a lightweight JDBC framework, JSQL means `Just SQL` and without ORM configuration.
It is a framework which is convenience and easy to use, just like sql syntax what you write as usual and 
makes Java SQL development more easier.

For Who ?

If you are a Java developer searching for the jdbc framework satisfy functions as connection pool, super lightweight orm
and want to write sql like programing java code.

Why JSQL ?
- No SQL string and make your code clean
- No ORM bean code generation mass your git control
- Provide ExecutorPool/ConnectionPool for jdbc connection pooling

[TOC]

## Requirements
- JDK8+

## Features
1. Connection Pool
2. SQL Builder
3. Support common dialects
4. Auto paging
5. Jdbc Executor for query, update or batch update
6. Super lightweight ORM
7. Against SQL inject

## Quick Start
As following example, you can learn how to new a Connection from JSQLDataSource, Build SQL with Builder, and finally execute with JdbcExecutor.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (Connection connection = dataSource.newConnection()) {
    JdbcExecutor executor = new DefaultJdbcExecutor(connection);
    Builder builder = new SelectBuilder() {{
        select().from("table").where().eq("name", "jsql").build();
    }};
    List<Map<String, Object>> resultList = executor.execQuery(builder);
}
```

maybe you just need `JdbcExecutor` rather than `Connection`, we could simplfy our example as follow
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (JdbcExecutor executor = dataSource.createJdbcExecutor(connection)) {
    Builder builder = new SelectBuilder() {{
        select().from("table").where().eq("name", "jsql").build();
    }};
    List<Map<String, Object>> resultList = executor.execQuery(builder);
}
```

JSQLDataSource should be singleton per url/username

## JSQLDataSource
Now, let’s try to create a JSQLDataSource and new a Connection from JSQLDataSource.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
Connection connection = dataSource.newConnection();
// TODO do something with connection
```

### Connection Pool
First, we'd create a JSQLDataSource, and then invoke `JSQLDataSource.createConnectionPool` to create a Connection Pool.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
ConnectionPool pool = dataSource.createConnectionPool();
Connection connection = null;
try {
    connection = pool.getConnection();
    // TODO do something with connection
} finally {
    pool.returnConnection(connection);
}
```

### JdbcExecutor Pool
Like Connection Pool creation, but provide a executor object which combine JdbcExecutor and Connection to work together.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
JdbcExecutorPool pool = dataSource.createExecutorPool();
try (JdbcExecutor executor = pool.getExecutor()) {
    // TODO do something with executor
}
```

Let's refactor our quick start example by using `JdbcExecutorPool`
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
JdbcExecutorPool pool = dataSource.createExecutorPool();
try (JdbcExecutor executor = pool.getExecutor()) {
    Builder builder = new SelectBuilder() {{
        select().from("table").where().eq("name", "jsql").build();
    }};
    List<Map<String, Object>> resultList = executor.execQuery(builder);
}
```

### Pool Configuration
If we want to configure pool, do it by `PoolConfiguration`.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");

PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
poolConfiguration.setMaxPoolSize(32);
ObjectPool<Connection> pool = dataSource.createConnectionPool(poolConfiguration);
```

 name | comment | default value
---|---|---
maxPoolSize | max object pool size | 12
idleTimeout | The max valid time between object returned time and now with milliseconds, 0 will be timeout immedately when checked by pool maintainer, but -1 will never timeout | -1 
idleCheckInterval | The interval time of milliseconds to trigger pool maintainer checking idle object | 15 minus
pollTimeout | Setting time waiting for borrowing a object with milliseconds, -1 will never timeout | -1

`PoolConfiguration.defaultPoolCfg()` could get the default Pool Configuration.

Object Pool should be static final, borrow and return object globally

## Builder
Builder is responsible for SQL generation, like DQL and DML syntax, such as `select`/`insert`/`update`/`delete`. The best way to create a Builder instance is using `JSQLDataSource` which will set `Dialect` into Builder automatically. Let's refer as following examples to understand it's usage.
```java
JSQLDataSource dataSource = new JSQLDataSource("jdbc:mysql://host:3306/database", "username", "password");
SelectBuilder select = dataSource.selectBuilder(); // same to new SelectBuilder(Dialects.MYSQL);
UpdateBuilder update = dataSource.updateBuilder(); // same to new UpdateBuilder(Dialects.MYSQL);
InsertBuilder insert = dataSource.insertBuilder(); // same to new InsertBuilder(Dialects.MYSQL);
DeleteBuilder delete = dataSource.deleteBuilder(); // same to new DeleteBuilder(Dialects.MYSQL);
```

### SelectBuilder
```java
Builder builder = new SelectBuilder() {{
    select().from("table").where().eq("name", "jsql").build();
}};
```
**SQL**: select * from table where name = ?

**VALUE**: "jsql"

### InsertBuilder
```java
Builder insert = new InsertBuilder() {{
    insertInto("table")
        .values(
            Cond.eq("col1", "val1"),
            Cond.eq("col2", 102),
            Cond.eq("col3", "val3"))
        .build();
}};
```
**SQL**: insert into t_table(col1,col2,col3) values(?,?,?)

**VALUE**: "val1", 102, "val3"

### UpdateBuilder
```java
Builder update = new UpdateBuilder() {{
    update("t_table")
        .set(
            Cond.eq("col1", "val1"),
            Cond.eq("col2", 102),
            Cond.eq("col3", "val3"))
        .where()
        .like("id", "123%")
        .build();
}};
```
**SQL**: update t_table set col1 = ?, col2 = ?, col3 = ? where id like ?

**VALUE**: "val1", 102, "val3"

### DeleteBuilder
```java
Builder delete = new DeleteBuilder() {{
    delete().from("t_table").where().eq("id", 123456789).build();
}};
```
**SQL**: delete from t_table where id = ?

**VALUE**: 123456789

## Condition
There are variant condition and you will find out the method name is same to SQL syntax. Condition is used in Builder, sometimes also used as value. Here docs is for special condition, such as `gt(>)`/`ge(>=)`/`lt(<)`/`le(<=)`/`like`/`ne(<>)`/`eq(=)` I thought you can handle it yourself.

### var
Sometimes, we need to make condition without place holder which display in SQL with `?`, but directly display in SQL, just like `Cond.var("key", "value")` output `key = value`, check example as follow.

```java
Builder builder = new SelectBuilder() {{
    select()
        .from("t_table", "t_table1")
        .where()
        .var("t_table.id", "t_table1.id")
        .build();
}};
```
**SQL**: select * from t_table, t_table1 where `t_table.id=t_table1.id`

### groupBy/having
```java
Builder builder = new SelectBuilder() {{
    select("name", "age")
        .from("t_table")
        .groupBy("name", "age").having(Cond.gt("age", 18))
        .build();
}};
```
**SQL**: select name, age from t_table group by name,age having ( age > ?)
**VALUE**: 18

### orderBy
```java
Builder builder = new SelectBuilder() {{
    select("name", "age")
        .from("t_table")
        .orderBy("name desc", "age")
        .build();
}};
```
**SQL**: select name, age from t_table order by name desc,age

### isNull/isNotNull
```java
builder = new SelectBuilder() {{
    select("name", "age")
        .from("t_table")
        .where()
        .isNull("name")
        .and()
        .isNotNull("age")
        .build();
}};
```
**SQL**: select name, age from t_table where name is null and age is not null

### forUpdate
```java
builder = new SelectBuilder() {{
    select("name", "age")
        .from("t_table")
        .where()
        .eq("id", 123456789)
        .forUpdate()
        .build();
}};
```
**SQL**: select name, age from t_table where id = ? for update

**VALUE**: 123456789

### union
```java
Builder select = new SelectBuilder() {{
    select("t_id as id", "t_name as name")
        .from("table")
        .where()
        .eq("region", "Canton")
        .union(
            new SelectBuilder() {{
                select("id", "name").from("table_1").where().eq("region", "China").build();
        }})
        .build();
}};
```
**SQL**: select t_id as id, t_name as name from table where region = ? union select id, name from table_1 where region = ?

**VALUE**: "Canton", "China"

### unionAll
```java
Builder select = new SelectBuilder() {{
    select("t_id as id", "t_name as name")
        .from("table")
        .where()
        .eq("region", "Canton")
        .unionAll(
            new SelectBuilder() {{
                select("id", "name").from("table_1").where().eq("region", "China").build();
        }})
        .build();
}};
```
**SQL**: select t_id as id, t_name as name from table where region = ? union all select id, name from table_1 where region = ?

**VALUE**: "Canton", "China"

### and/or
Sometimes we need to resolve multi conditions combination, so that `and(Condition... conditions)` and `or(Condition... conditions)` come out, as following example you can find out their usage.
```java
builder = new SelectBuilder() {{
    select("name", "age")
        .from("t_table")
        .where()
        .and(Cond.like("name", "%Lee"), Cond.gt("age", 18))
        .and()
        .or(Cond.eq("age", 12), Cond.eq("age", 16))
        .build();
}};
```
**SQL**: select name, age from t_table where ( name like ? and age > ?) and ( age = ? or age = ?)

**VALUE**: "%Lee", 18, 12, 16


### exists
```java
Builder existsSelect = new SelectBuilder() {{
    select("1")
        .from("t_table1")
        .where()
        .var("t_table.id", "t_table1.id")
        .build();
}};
Builder builder = new SelectBuilder() {{
    select()
        .from("t_table")
        .where()
        .exists(existsSelect)
        .build();
}};
```
**SQL**: select * from t_table where exists (select 1 from t_table1 where t_table.id=t_table1.id)

### notExists
```java
Builder existsSelect = new SelectBuilder() {{
    select("1")
        .from("t_table1")
        .where()
        .var("t_table.id", "t_table1.id")
        .build();
}};
Builder builder = new SelectBuilder() {{
    select()
        .from("t_table")
        .where()
        .notExists(existsSelect)
        .build();
}};
```
**SQL**: select * from t_table where not exists (select 1 from t_table1 where t_table.id=t_table1.id)

### in
#### with Array
```java
Builder select = new SelectBuilder() {{
    select().from("table").where().in("lang", "TW", "CN", "HK").build();
}};
```
**SQL**: select * from table where lang in (?,?,?)

**VALUE**: "TW", "CN", "HK"

#### with `java.util.Collection`
```java
Builder select = new SelectBuilder() {{
    select().from("table").where().in("lang", new LinkedList<String>() {{
        add("TW");
        add("CN");
        add("HK");
    }}}).build();
}};
```
**SQL**: select * from table where lang in (?,?,?)
 
**VALUE**: "TW", "CN", "HK"

#### with `SelectBuilder`
```java
Builder selectIn = new SelectBuilder() {{
    select("name").from("table_1").where().like("name", "%jsql%").build();
}};
Builder select = new SelectBuilder() {{
    select().from("table").where().in("name", selectIn).build();
}};
```
**SQL**: select * from table where name in (select name from table_0 where name like ?)

**VALUE**: "%jsql%"

### Join Table
#### inner Join
```java
Builder builder = new SelectBuilder() {{
    select()
    .from("table_1").joinOn("table_2", Cond.var("table1.id", "table2.id"))
    .build();
}};
```
**SQL**: select * from table_1 join table_2 on `table1.id=table2.id`

#### left Join
```java
Builder builder = new SelectBuilder() {{
    select()
    .from("table_1").leftJoinOn("table_2", Cond.var("table1.id", "table2.id"))
    .build();
}};
```
**SQL**: select * from table_1 left join table_2 on `table1.id=table2.id`

#### right Join
```java
Builder builder = new SelectBuilder() {{
    select()
    .from("table_1").rightJoinOn("table_2", Cond.var("table1.id", "table2.id"))
    .build();
}};
```
**SQL**: select * from table_1 right join table_2 on `table1.id=table2.id`

#### outer Join
```java
Builder builder = new SelectBuilder() {{
    select()
    .from("table_1").outerJoinOn("table_2", Cond.var("table1.id", "table2.id"))
    .build();
}};
```
**SQL**: select * from table_1 outer join table_2 on `table1.id=table2.id`

#### full join
```java
Builder builder = new SelectBuilder() {{
    select()
    .from("table_1").fullJoinOn("table_2", Cond.var("table1.id", "table2.id"))
    .build();
}};
```
**SQL**: select * from table_1 full join table_2 on `table1.id=table2.id`

### offset/limit
Offset and limit will make different paging SQL by different Dialect, as following example will show you MySQL and Oracle Dialect offset and limit setting. Only set offset without limit is forbidden, but only set limit is allow, e.g. `Builder.offset(6).build()` will occur error.

#### MySQL
```java
Builder select = new SelectBuilder(Dialects.MYSQL) {{
    select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
}};
```
**SQL**: select * from table where id = ? limit ?,?

**VALUE**: "0123456789", 5, 10

```java
Builder select = new SelectBuilder(Dialects.MYSQL) {{
    select().from("table").where().eq("id", "0123456789").limit(10).build();
}};
```
**SQL**: select * from table where id = ? limit ?

**VALUE**: "0123456789", 10

#### Oracle
```java
Builder select = new SelectBuilder(Dialects.ORACLE) {{
    select().from("table").where().eq("id", "0123456789").offset(5).limit(10).build();
}};
```
**SQL**: select * from ( select _source.*, rownum _rownum from (select * from table where id = ? ) _source where rownum <= ?) where _rownum > ?

**VALUE**: "0123456789", 10, 5

```java
Builder select = new SelectBuilder(Dialects.ORACLE) {{
    select().from("table").where().eq("id", "0123456789").limit(10).build();
}};
```
**SQL**: select * from ( select * from table where id = ? ) where rownum <= ?

**VALUE**: "0123456789", 10

## Dialect
Dialect is for unify variant DB while building SQL, so far, Dialect could inject offset and limit, specify driver class name.

### Support Dialects
- CubridDialect
- SQLiteDialect
- DB2Dialect
- DerbyDialect
- H2Dialect
- MariadbDialect
- MySQLDialect
- OracleDialect
- PostgreSQLDialect
- SQLServer2012PlusDialect
- SqlServerDialect
- UnknownDialect

### Custom your own Dialect
1. Implements Dialect interface
```java
public CustomDialect implements Dialect {
    public String getDriverClassName() {
        return "my.custom.jdbc.MyDriver";
    }
    public void injectOffsetLimit(BuilderContext builderCtx) {
        boolean offsetExists = builderContext.getOffset() > 0;
        StringBuilder offsetLimitBuilder = new StringBuilder(offsetExists ? " limit ?,?" : " limit ?");
        if (offsetExists) {
            builderContext.addCondition(Cond.value(builderContext.getOffset()));
        }
        builderContext.addCondition(Cond.value(builderContext.getLimit()));

        StringBuilder preparedSqlBuilder = builderContext.getPreparedSql();
        if (builderContext.getForUpdatePosition() > 0) {
            preparedSqlBuilder.insert(builderContext.getForUpdatePosition(), offsetLimitBuilder);
        } else {
            preparedSqlBuilder.append(offsetLimitBuilder);
        }
    }
    public boolean supportOffsetLimit() {
        return true;
    }
}
```
2. How to make `CustomDialect` work
```java
JSQLDataSource datasource = new JSQLDataSource(url, username, password, new CustomDialect());

Builder builder = new SelectBuilder(new CustomDialect());
```

## ORM
You can use simple orm helper utily which name is `cn.icuter.jsql.orm.ORMapper`. Using `@ColumnName` annotation associate db table column and object field name.

### Select Value
`cn.icuter.jsql.executor.DefaultJdbcExecutor.execQuery(Builder, Class<T>)` map to customer bject

```java
Builder builder = new SelectBuilder() {{
    select().from("t_table t").build();
}};
JdbcExecutor jdbcExecutor = new DefaultJdbcExecutor(connection);
List<Map<String, Object>> resultMap = jdbcExecutor.execQuery(builder);
List<OrgUnit> resultORM = jdbcExecutor.execQuery(builder, Table.class);
```

### Insert Value
If field value is null will be ignore
```java
Student student = new Student();
student.setGrade(2);
student.setName("Edward");
student.setAge(20);
student.setClass("04-1");

Builder insert = new InsertBuilder() {{
    insertInto("t_student").values(student).build();
}};
```

### Update Value
If field value is null will be ignore
```java
Student student = new Student();
student.setGrade(2);
student.setName("Edward");
student.setAge(20);
student.setClass(null); // don't set class null

Builder update = new UpdateBuilder() {{
    update("t_student").set(student).build();
}};
```

If you want to set null value, you can use `ORMapper.toMap` instead of `ORMapper.toMapIgnoreNullValue`
```java
Student student = new Student();
student.setGrade(2);
student.setName("Edward");
student.setAge(20);
student.setClass(null);

ORMapper studentMapper = new ORMapper(student);
Builder update = new UpdateBuilder() {{
    update("t_student").set(studentMapper.toMap()).build();
}};
```

## JdbcExecutor
When Builder has been built, we could use JdbcExecutor for execution. `dataSource.createJdbcExecutor()` will create a `CloseableJdbcExecutor` object, while calling `close` method, `Conection` in `JdbcExecutor` will be closed as well.

### Select
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (JdbcExecutor executor = dataSource.createJdbcExecutor()) {
    Builder builder = new SelectBuilder() {{
        select().from("table").where().eq("name", "jsql").build();
    }};
    List<Map<String, Object>> resultList = executor.execQuery(builder);
}
```

### Insert/Update/Delete
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (JdbcExecutor executor = dataSource.createJdbcExecutor()) {
    Builder builder = new DeleteBuilder() {{
        delete().from("table").where().eq("name", "jsql").build();
    }};
    int count = executor.execUpdate(builder);
}
```

### Batch Update
Builder List in batch group by the same sql, if sql is different, will seperate different batch update.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (JdbcExecutor executor = dataSource.createJdbcExecutor()) {
    List<Builder> builderList = new LinkedList<>() {{
        add(new DeleteBuilder() {{
            delete().from("table").where().eq("name", "Jhon").build();
        }});
        add(new DeleteBuilder() {{
            delete().from("table").where().eq("name", "Edward").build();
        }});
        add(new DeleteBuilder() {{
            delete().from("table").where().eq("name", "Jack").build();
        }});
    }};
    executor.execBatch(builderList);
}
```

As following example, I try to delete and update in `execBatch` with different executable SQL.
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (JdbcExecutor executor = dataSource.createJdbcExecutor()) {
    List<Builder> builderList = new LinkedList<>() {{
        add(new DeleteBuilder() {{
            delete().from("table").where().eq("name", "Jhon").build();
        }});
        add(new DeleteBuilder() {{
            delete().from("table").where().eq("name", "Edward").build();
        }});
        add(new UpdateBuilder() {{
            update("table").set(Cond.eq("name", "Edward")).where().eq("id", 12345678).build();
        }});
        add(new UpdateBuilder() {{
            update("table").set(Cond.eq("name", "John")).where().eq("id", 123456789).build();
        }});
    }};
    executor.execBatch(builderList);
}
```
At this time, `delete from table where name = ?` and `update table set name = ? where id = ?` will create two batch execution.

### Note
If you want to new a `JdbcExcecutor` instance without `JSQLDataSource`, you could pass your `Connection` as parameter to do that.
```java
Connection connection = yourConnection;
JdbcExecutor executor = new DefaultJdbcExecutor(connection);
Builder builder = new SelectBuilder() {{
    select().from("table").where().eq("name", "jsql").build();
}};
List<Map<String, Object>> resultList = executor.execQuery(builder);
```

## Transaction
### TransactionExecutor From JSQLDataSource
We could create a `TransactionExecutor` by `JSQLDataSource`, as following example, we could learn how to start with transaction.
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
TransactionExecutor executor = dataSource.createTransaction();
try {
    executor.execUpdate(new InsertBuilder() {{
        insertInto("t_jsql_test").values(
                Cond.eq("t_col_1", "transaction-val-1"),
                Cond.eq("t_col_2", "transaction-val-1")
        ).build();
    }});
    executor.execUpdate(new InsertBuilder() {{
        insertInto("t_jsql_test").values(
                Cond.eq("t_col_1", "transaction-val-2"),
                Cond.eq("t_col_2", "transaction-val-2")
        ).build();
    }});
    executor.commit();
} catch (Exception e) {
    e.printStackTrace();
    executor.rollback();
}
```
**Note**
> Please note that, Connection will be closed while calling commit or rollback, if don't commit or rollback, Conneciton in TransactionExecutor will be always alive and keep connecting to DB server.

### TransactionExecutor From JdbcExecutorPool
From JdbcExecutor Pool we could get `TransactionExecutor` for transaction operation, `try () {}` or `TransactionExecutor.close` will auto return `TransactionExecutor` back to the pool. If you forgot to commit, all DML to DB will be rolled back while returning to pool.
```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
JdbcExecutorPool pool = dataSource.createExecutorPool();
try (JdbcExecutor executor = pool.getTransactionExecutor()) {
    // TODO do something with executor
    executor.commit(); // you have to commit after opertion done, otherwise will be rolled back
}
```