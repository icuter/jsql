JSQL
======

Welcome to JSQL world. There is a light JDBC framework, JSQL means `Just SQL` and without ORM configuration.
This is a very light and easy to use, just like sql syntax what you write as usual and 
makes Java SQL develop more easy and independently

### features
1. Connection Pool
2. SQL Builder
3. Support common dialects
4. Auto paging
5. Jdbc Executor for query, update or batchUpdate
6. ORM

### Builder
#### sample
```java
Builder builder = new SelectBuilder() {{
    select().from("table").where().eq("name", "jsql").build();
}};
```
> SQL: select * from table where name = ?
> 
> VALUE: "jsql"