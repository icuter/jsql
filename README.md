JSQL
======

Welcome to JSQL world. There is a light JDBC framework, JSQL means `Just SQL` and without ORM configuration.
It is a framework which is very light and easy to use, just like sql syntax what you write as usual and 
makes Java SQL development more easy.

For Who ?

If you are Java developer searching for the jdbc framework satisfy functions as connection pool, super light orm
and want to write sql like programing java code.

Why JSQL ?
- No SQL string and make your code clean
- No ORM bean code generation mass your git control
- Provide ExecutorPool/ConnectionPool for jdbc connection pooling

### Requirements
- JDK8+

### Features
1. Connection Pool
2. SQL Builder
3. Support common dialects
4. Auto paging
5. Jdbc Executor for query, update or batch update
6. Super light ORM
7. Against SQL inject

### Quick Start
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

> We recommend JSQLDataSource is a singleton object

### JSQLDataSource
Now, let’s try to create a JSQLDataSource and new a Connection from JSQLDataSource.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
Connection connection = dataSource.newConnection();
// TODO do something with connection
```

#### Connection Pool
First, we'd create a JSQLDataSource, and then invoke `JSQLDataSource.createConnectionPool` to create a Connection Pool.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
ObjectPool<Connection> pool = dataSource.createConnectionPool();

Connection connection = null;
try {
    connection = pool.borrowObject();
    // TODO do something with connection
} finally {
    pool.returnObject(connection);
}
```

#### JdbcExecutor Pool
Like Connection Pool creation, but provide a executor object which combine JdbcExecutor and Connection to work together.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
ObjectPool<JdbcExecutor> pool = dataSource.createExecutorPool();

JdbcExecutor executor = null;
try {
    executor = pool.borrowObject();
    // TODO do something with executor
} finally {
    pool.returnObject(executor);
}
```

#### Pool Configuration
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

> `PoolConfiguration.defaultPoolCfg()` could get the default Pool Configuration.

> JSQLDataSource should be singleton
> 
> Object Pool should be static final, borrow and return object globally


### Builder
#### Simplest Sample
Let's new a simplest Builder instance and you can find out the method name is same to SQL syntax.

```java
Builder builder = new SelectBuilder() {{
    select().from("table").where().eq("name", "jsql").build();
}};
```
> **SQL**: select * from table where name = ?
> 
> **VALUE**: "jsql"

#### Var
Sometimes, we need to make condition without place holder which display in SQL with `?`, but directly display in SQL, just like `Cond.var("key", "value")` output `key = value`, check example as follow.

```java
Builder existsSelect = new SelectBuilder() {{
    select()
        .from("t_table", "t_table1")
        .where()
        .var("t_table.id", "t_table1.id")
        .and()
        .eq("t_table1.name", "jsql")
        .build();
}};
```
> **SQL**: select * from t_table, t_table1 where t_table.id=t_table1.id and t_table1.name = ?)
> 
> `var("t_table.id", "t_table1.id")` display in SQL is `t_table.id=t_table1.id`
> 
> **VALUE**: "jsql"

#### Exists
```java
Builder existsSelect = new SelectBuilder() {{
    select("1")
        .from("t_table1")
        .where()
        .var("t_table.id", "t_table1.id")
        .and().eq("t_table1.name", "jsql")
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
> **SQL**: select * from t_table where exists (select 1 from t_table1 where t_table.id=t_table1.id and t_table1.name = ?)
> 
> **VALUE**: "jsql"

#### Not Exists
```java
Builder existsSelect = new SelectBuilder() {{
    select("1")
        .from("t_table1")
        .where()
        .var("t_table.id", "t_table1.id")
        .and().eq("t_table1.name", "jsql")
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
> **SQL**: select * from t_table where not exists (select 1 from t_table1 where t_table.id=t_table1.id and t_table1.name = ?)
> 
> **VALUE**: "jsql"

#### In
##### With Array
```java
Builder select = new SelectBuilder() {{
    select().from("table").where().in("lang", "TW", "CN", "HK").build();
}};
```
> **SQL**: select * from table where lang in (?,?,?)
> 
> **VALUE**: "TW", "CN", "HK"

##### With java.util.Collection
```java
Builder select = new SelectBuilder() {{
    select().from("table").where().in("lang", new LinkedList<String>() {{
        add("TW");
        add("CN");
        add("HK");
    }}}).build();
}};
```
> **SQL**: select * from table where lang in (?,?,?)
> 
> **VALUE**: "TW", "CN", "HK"

##### With SelectBuilder
```java
Builder selectIn = new SelectBuilder() {{
    select("name").from("table_1").where().like("name", "%jsql%").build();
}};
Builder select = new SelectBuilder() {{
    select().from("table").where().in("name", selectIn).build();
}};
```
> **SQL**: select * from table where name in (select name from table_0 where name like ?)
> 
> **VALUE**: "%jsql%"

#### Dialect
Dialect is for unify variant DB while building SQL, so far, Dialect could inject offset and limit, specify driver class name.

##### Support Dialects
- CubridDialect
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

##### Custom your own Dialect
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
            builderContext.getConditionList().add(Cond.value(builderContext.getOffset()));
        }
        builderContext.getConditionList().add(Cond.value(builderContext.getLimit()));

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

### JdbcExecutor
When Builder has been built, we could use JdbcExecutor for execution.

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
try (Connection connection = dataSource.newConnection()) {
    JdbcExecutor executor = new JdbcExecutor(connection);
    Builder builder = new SelectBuilder() {{
        select().from("table").where().eq("name", "jsql").build();
    }};
    List<Map<String, Object>> resultList = executor.execQuery(builder);
}
```