JSQL
======

## License
[MIT licensed](https://github.com/icuter/jsql/blob/master/LICENSE.md).


## Abstract

Welcome to JSQL world. It's a lightweight JDBC framework, JSQL means `Just SQL` and without ORM configuration.
It is a framework which is convenience and easy to use, just like sql syntax what you write as usual and 
makes Java SQL development more easier.

Who ?

If you are a Java developer searching for the jdbc framework satisfy functions as connection pool, super lightweight orm
and want to write sql like java code programing, then I suggest you could try to use JSQL framework for jdbc operation.

Why ?
- No SQL string and keep your code graceful
- No ORM bean code generation mass your git control
- Provide ExecutorPool/ConnectionPool for jdbc connection pooling without DBCP dependencies

## Requirements

- JDK6 or higher

## Features
1. Connection/JdbcExecutor pool
2. SQL syntax like builder
3. Transaction
4. Support common dialects
5. Auto paging
6. Jdbc executor for query, update or batch update
7. Super lightweight ORM
8. Against SQL inject
9. Logging ability


## Quick Start

### Maven dependency
```xml
<!-- for jdk1.8+ -->
<dependency>
  <groupId>cn.icuter</groupId>
  <artifactId>jsql</artifactId>
  <version>1.0.5</version>
</dependency>

<!-- for jdk1.6+ -->
<dependency>
  <groupId>cn.icuter</groupId>
  <artifactId>jsql-jdk1.6</artifactId>
  <version>1.0.5</version>
</dependency>
```

### Examples

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
List<Map<String, Object>> list = dataSource.select()
                                           .from("table")
                                           .where().eq("name", "jsql")
                                           .execQuery();
```

```text
SQL: select * from table where name = ?
Value: [jsql]
```

### Transaction

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
dataSource.transaction(tx -> {
    tx.insert("table")
      .values(Cond.eq("col1", "val1"), Cond.eq("col2", 102),Cond.eq("col3", "val3"))
      .execUpdate();
    // tx.commit(); // auto commit if transaction ended
});
```

```text
SQL: insert into table(col1,col2,col3) values(?,?,?)
VALUE: ["val1", 102, "val3"]
```

Sometimes, Transaction with ThreadLocal to control full chain of request

```java
JSQLDataSource dataSource = new JSQLDataSource("url", "username", "password");
TransactionDataSource tx = dataSource.transaction();
tx.insert("table")
  .values(Cond.eq("col1", "val1"), Cond.eq("col2", 102),Cond.eq("col3", "val3"))
  .execUpdate();
tx.close(); // auto commit
```

```text
SQL: insert into table(col1,col2,col3) values(?,?,?)
VALUE: ["val1", 102, "val3"]
```

## Documents
Find more documentation [here](https://www.icuter.cn).

1. [JDBC Connection Pool](https://www.icuter.cn/pool.html)
1. [Transaction](https://www.icuter.cn/transaction.html)
2. [SQL Builder](https://www.icuter.cn/builder.html)
3. [Condition](https://www.icuter.cn/condition.html)
4. [DB Dialect](https://www.icuter.cn/dialect.html)
5. [ORM](https://www.icuter.cn/orm.html)
6. [SQL Executor](https://www.icuter.cn/executor.html)
7. [Logging Customization](https://www.icuter.cn/logging.html)

## Release Notes
### 1.0.5
bugfix:
- fix top-select in `SelectBuilder.select`

feature:
- support transaction in JSQLDataSource
- support `insert... select...` syntax


### 1.0.4
bugfix:
- fix pool configuration

feature:
- execute builder directly in JSQLDataSource
- refactor Connection object idle timeout validation

### 1.0.3
break:
- Remove `Builder.union` and `Builder.unionAll` operation

bugfix:
- fix `OracleDialect` invalid table alias name format
- fix `DB2Dialect` invalid table alias name format

feature:
- Add builder as Condition value
- Add `UnionSelectBuilder` for union/unionAll operation
