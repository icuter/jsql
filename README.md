JSQL
======

## License
[MIT licensed](https://github.com/icuter/jsql/blob/master/LICENSE.md).


## Abstract

Welcome to JSQL. It's a lightweight JDBC DSL framework, JSQL means `Just SQL` and without ORM configuration.
It is a framework which is convenience and easy to use, just like sql syntax that you feel free to write SQL as usual and 
makes Java SQL development more easier.

If you are a Java developer searching for the jdbc framework satisfy functions as connection pool, super lightweight orm
and want to write sql like java code programing, then I suggest you could try to use JSQL framework for jdbc operation.

JSQL for Reasons:
- No SQL string and keep your code graceful
- No ORM bean code generation mass your git control
- Provide ExecutorPool/ConnectionPool for jdbc connection pooling without DBCP dependencies

## Requirements

- JDK6 or higher

## Features
1. Connection/JdbcExecutor pool
2. SQL syntax like builder
3. Transaction
4. Support customizing dialects
5. Pagination
6. Jdbc executor for query, update or batch update
7. Super lightweight ORM
8. Against SQL inject
9. Logging ability

## Support Databases
1. Cubrid
2. SQLite
3. DB2
4. Derby (EmbeddedDerby/NetworkDerby)
5. H2
6. MariaDB
7. MySQL
8. Oracle
9. PostgreSQL
10. SQLServer2012(version >= 2012)

## Quick Start

### Maven dependency
```xml
<!-- for jdk1.8+ -->
<dependency>
  <groupId>cn.icuter</groupId>
  <artifactId>jsql</artifactId>
  <version>1.0.6</version>
</dependency>

<!-- for jdk1.6+ -->
<dependency>
  <groupId>cn.icuter</groupId>
  <artifactId>jsql-jdk1.6</artifactId>
  <version>1.0.6</version>
</dependency>
```

### Examples

### Auto Commit

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

Using standalone Transaction to control commit or rollback operation as your favour

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

**NOTE**
> Above examples are using JSQLDataSource inner Connection pool to execute SQL generated by JSQL.

## Documents
Find more documentation [here](https://www.icuter.cn).

1. [DataSource](https://www.icuter.cn/datasource.html)
2. [JDBC Connection Pool](https://www.icuter.cn/pool.html)
3. [Transaction](https://www.icuter.cn/transaction.html)
4. [SQL Builder](https://www.icuter.cn/builder.html)
5. [Condition](https://www.icuter.cn/condition.html)
6. [DB Dialect](https://www.icuter.cn/dialect.html)
7. [ORM](https://www.icuter.cn/orm.html)
8. [SQL Executor](https://www.icuter.cn/executor.html)
9. [Logging Customization](https://www.icuter.cn/logging.html)

## Release Notes
### 1.0.6
bug fixes
- fix top-select in `SelectBuilder.select`

features
- support transaction in JSQLDataSource
- support `insert... select...` syntax
- support Driver properties when getting Connection from Driver

### 1.0.4
bug fixes
- fix pool configuration

features
- execute builder directly in JSQLDataSource
- refactor Connection object idle timeout validation

> jsql-jdk1.6 missing this version

### 1.0.3
breaks
- Remove `Builder.union` and `Builder.unionAll` operation

bug fixes
- fix `OracleDialect` invalid table alias name format
- fix `DB2Dialect` invalid table alias name format

features
- Add builder as Condition value
- Add `UnionSelectBuilder` for union/unionAll operation

> jsql-jdk1.6 missing this version