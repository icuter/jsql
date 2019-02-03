JSQL
======

## License
[MIT licensed](https://github.com/icuter/jsql/blob/master/LICENSE.md).

Welcome to JSQL world. It's a lightweight JDBC framework, JSQL means `Just SQL` and without ORM configuration.
It is a framework which is convenience and easy to use, just like sql syntax what you write as usual and 
makes Java SQL development more easier.

For Who ?

If you are a Java developer searching for the jdbc framework satisfy functions as connection pool, super lightweight orm
and want to write sql like java code programing, then I suggest you could try to use JSQL framework for jdbc operation.

Why JSQL ?
- No SQL string and make your code clean
- No ORM bean code generation mass your git control
- Provide ExecutorPool/ConnectionPool for jdbc connection pooling

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

## Documents
To learn JSQL framework, just try to do it with [Quick Start](https://www.icuter.cn/quickstart.html), and you can find the documentation [here](https://www.icuter.cn).

1. [JDBC Connection Pool](https://www.icuter.cn/pool.html)
2. [SQL Builder](https://www.icuter.cn/builder.html)
3. [Condition](https://www.icuter.cn/condition.html)
4. [DB Dialect](https://www.icuter.cn/dialect.html)
5. [ORM](https://www.icuter.cn/ORM.html)
6. [SQL Executor](https://www.icuter.cn/executor.html)
7. [Logging Customization](https://www.icuter.cn/logging.html)

## Release Notes
### 1.0.3
break:
- `Builder.union` and `Builder.unionAll` are disabled unless `UnionSelectBuilder` 

bugfix:
1. `OracleDialect` invalid table alias name
2. `DB2Dialect` invalid table alias name

feature:
1. Builder as Condition value
2. Add `UnionSelectBuilder` for union/unionAll operation
