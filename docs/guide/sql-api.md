# SQL API DSL

The SQL API DSL generates a Spring WebMVC API directly from SQL statements. It is intended for read models, reporting endpoints, and small SQL-backed commands where writing a controller, service interface, DTOs, and JDBC binding code by hand would add little value.

Generated SQL APIs follow the same layered shape as the WebMVC DSL:

- API interface
- Feign interface
- Controller
- Service interface
- Service implementation
- Request and response DTOs

The generated service implementation executes SQL through `ZygardeSqlExecutor` with named parameters such as `:keyword`, `:id`, and `:tenantId`.

## Gradle Setup

Create a code generation module and depend on the SQL API DSL module:

```kotlin
dependencies {
  implementation(project(":zygarde-sql-api-codegen-dsl"))
}
```

If the DSL references application classes, such as a context parameter resolver, also add the module that contains those classes:

```kotlin
dependencies {
  implementation(project(":zygarde-sql-api-codegen-dsl"))
  implementation(project(":todo-src-core"))
}
```

Configure the code generation entry point and output folders:

```kotlin
apply(plugin = "application")

val generatedDtoDir = project(":todo-dsl-generated-sql-api-dto").file("src/main/kotlin").absolutePath
val generatedApiInterfaceDir = project(":todo-dsl-generated-sql-api-interface").file("src/main/kotlin").absolutePath
val generatedFeignDir = project(":todo-dsl-generated-sql-api-feign").file("src/main/kotlin").absolutePath
val generatedControllerDir = project(":todo-dsl-generated-sql-api-controller").file("src/main/kotlin").absolutePath
val generatedServiceInterfaceDir = project(":todo-dsl-generated-sql-api-service-interface").file("src/main/kotlin").absolutePath
val generatedServiceImplDir = project(":todo-dsl-generated-sql-api-service-impl").file("src/main/kotlin").absolutePath

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.sqlapi.SqlApiDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.sql-api.dto.package=example.sqlapi.dto",
    "-Dzygarde.codegen.dsl.sql-api.api-interface.package=example.sqlapi.api",
    "-Dzygarde.codegen.dsl.sql-api.controller.package=example.sqlapi.controller",
    "-Dzygarde.codegen.dsl.sql-api.service-interface.package=example.sqlapi.service",
    "-Dzygarde.codegen.dsl.sql-api.service-impl.package=example.sqlapi.service.impl",
    "-Dzygarde.codegen.dsl.sql-api.dto.write-to=$generatedDtoDir",
    "-Dzygarde.codegen.dsl.sql-api.api-interface.write-to=$generatedApiInterfaceDir",
    "-Dzygarde.codegen.dsl.sql-api.feign-interface.write-to=$generatedFeignDir",
    "-Dzygarde.codegen.dsl.sql-api.controller.write-to=$generatedControllerDir",
    "-Dzygarde.codegen.dsl.sql-api.service-interface.write-to=$generatedServiceInterfaceDir",
    "-Dzygarde.codegen.dsl.sql-api.service-impl.write-to=$generatedServiceImplDir",
  )
}
```

The generated service implementation module must have runtime access to the SQL executor:

```kotlin
dependencies {
  implementation(project(":zygarde-sql-api"))
}
```

If the API uses `contextParam`, the generated service implementation resolves Spring beans through `zygarde-di`, so add:

```kotlin
dependencies {
  implementation(project(":zygarde-di"))
  implementation(project(":todo-src-core"))
}
```

## Defining an API

Create a class that extends `SqlApiDslCodegen`:

```kotlin
import zygarde.codegen.dsl.sqlapi.SqlApiDslCodegen

class TodoSqlApiCodegen : SqlApiDslCodegen() {
  override fun codegen() {
    sqlApi("TodoReportApi", "/api/todo-report") {
      database {
        dataSource("dataSource")
        transactionManager("transactionManager")
      }

      query("searchTodos", "/search") {
        sql(
          """
          select t.id as id, t.description as description
          from todo t
          where (:keyword is null or t.description like concat('%', :keyword, '%'))
          order by t.id
          """.trimIndent()
        )
        queryParam<String?>("keyword", description = "Search keyword")
        column<Int>("id")
        column<String>("description")
        request("SearchTodosReq")
        response("TodoReportDto")
      }
    }
  }
}
```

Run the codegen module:

```bash
./gradlew :todo-codegen-dsl-sql-api:run
```

## Parameter Sources

SQL named parameters are discovered from the SQL text. You can declare each parameter to control its Kotlin type and HTTP binding.

| DSL | HTTP or server source | Generated SQL value |
| --- | --- | --- |
| `param<T>("name")` | request DTO field | `req.name` |
| `bodyParam<T>("name")` | request body DTO field | `req.name` |
| `pathParam<T>("name")` | path variable | method argument |
| `queryParam<T>("name")` | query parameter | method argument |
| `contextParam<T>(...)` | server-side resolver | resolver result |

If a SQL parameter is not declared, it defaults to nullable `String` and is treated like `param<String?>`.

Queries do not support `bodyParam`; use `queryParam`, `pathParam`, `param`, or `contextParam` instead. Commands may use `bodyParam` for `POST`, `PUT`, and `PATCH`.

Declared parameters must be used by SQL. This validation also applies to context parameters, so a declared `contextParam("tenantId", ...)` must appear in the main SQL or, for paged queries, the count SQL.

## Result Shapes

Queries default to returning a collection:

```kotlin
query("searchTodos", "/search") {
  // ...
  response("TodoReportDto")
}
```

Generated signature:

```kotlin
fun searchTodos(keyword: String?): Collection<TodoReportDto>
```

Use `returnsOne()` or `returnsOneOrNull()` for single-row queries:

```kotlin
query("findTodo", "/find/{id}") {
  sql(
    """
    select t.id as id, t.description as description
    from todo t
    where t.id = :id
    """.trimIndent()
  )
  pathParam<Int>("id")
  column<Int>("id")
  column<String>("description")
  response("TodoReportDto")
  returnsOneOrNull()
}
```

Generated signature:

```kotlin
fun findTodo(id: Int): TodoReportDto?
```

For pagination, the main SQL must use `:pageSize` and `:offset`; the count SQL must select the configured count column, which defaults to `totalCount`.

```kotlin
query("pageTodos", "/page") {
  sql(
    """
    select t.id as id, t.description as description
    from todo t
    where (:keyword is null or t.description like concat('%', :keyword, '%'))
    order by t.id
    limit :pageSize offset :offset
    """.trimIndent()
  )
  returnsPage(
    countSql = """
      select count(*) as totalCount
      from todo t
      where (:keyword is null or t.description like concat('%', :keyword, '%'))
    """.trimIndent()
  )
  queryParam<String?>("keyword")
  queryParam<Int>("atPage")
  queryParam<Int>("pageSize")
  column<Int>("id")
  column<String>("description")
  response("TodoReportDto")
}
```

Generated signature:

```kotlin
fun pageTodos(
  keyword: String?,
  pageSize: Int,
  atPage: Int,
): PageDto<TodoReportDto>
```

## Commands

Commands generate write endpoints and service implementations around `execute` or `insertAndReturnKey`.

```kotlin
command("createTodo", "/create") {
  sql(
    """
    insert into todo(description, check_times)
    values (:description, 0)
    """.trimIndent()
  )
  bodyParam<String>("description", description = "Todo description")
  request("CreateTodoBySqlReq")
  returnsGeneratedKey<Int>("id", responseName = "CreatedTodoKeyDto")
}
```

Command methods default to `POST`. Use `put()`, `delete()`, `post()`, or `method(RequestMethod.PATCH)` to change the HTTP method.

Supported command results:

- `returnsAffectedRows()` returns the update count.
- `returnsNoContent()` executes SQL and returns `Unit`.
- `returnsGeneratedKey<T>()` is valid for `INSERT` statements and returns a generated-key DTO.

## Server-Side Context Parameters

Use `contextParam` for SQL parameters that must come from the server instead of the client, such as the current user, tenant, organization, data scope, or request-local context. Context parameters are not included in request DTOs, API interface method arguments, Feign method arguments, controller method arguments, or service interface method arguments.

First, define a resolver bean:

```kotlin
import org.springframework.stereotype.Component
import zygarde.sql.api.SqlApiContextParamResolver

@Component
class CurrentTenantIdResolver(
  private val tenantContext: TenantContext,
) : SqlApiContextParamResolver<String> {
  override fun resolve(paramName: String): String {
    return tenantContext.tenantId
  }
}
```

Then declare the SQL parameter as a context parameter:

```kotlin
query("findTenantTodos", "/tenant-todos") {
  sql(
    """
    select t.id as id, t.description as description
    from todo t
    where t.tenant_id = :tenantId
      and (:keyword is null or t.description like concat('%', :keyword, '%'))
    order by t.id
    """.trimIndent()
  )
  queryParam<String?>("keyword")
  contextParam<String>("tenantId", resolver = CurrentTenantIdResolver::class)
  column<Int>("id")
  column<String>("description")
  response("TodoReportDto")
}
```

The generated API contract only exposes client-controlled inputs:

```kotlin
fun findTenantTodos(keyword: String?): Collection<TodoReportDto>
```

The generated service implementation resolves the context value and adds it to the SQL parameter map:

```kotlin
val params = mapOf(
  "keyword" to keyword,
  "tenantId" to bean<CurrentTenantIdResolver>().resolve("tenantId"),
)
```

### Resolver Declaration Forms

Use a resolver type when the resolver class is known at compile time:

```kotlin
contextParam<String>("tenantId", resolver = CurrentTenantIdResolver::class)
```

Use the generic form when you want the resolver type checked by Kotlin:

```kotlin
contextParam<String, CurrentTenantIdResolver>("tenantId")
```

Use a bean name when the resolver must be selected by Spring bean name:

```kotlin
contextParam<String>("tenantId", resolverBeanName = "currentTenantIdResolver")
```

Named resolvers are generated as a Spring bean lookup and cast to `SqlApiContextParamResolver<*>` before resolving the value.

## Generated Contract

For a context-filtered query like:

```kotlin
query("findCurrentTodo", "/current") {
  sql(
    """
    select t.id as id, t.description as description
    from todo t
    where t.id = :currentTodoId
    """.trimIndent()
  )
  contextParam<Int?>("currentTodoId", resolver = CurrentTodoIdResolver::class)
  column<Int>("id")
  column<String>("description")
  response("TodoReportDto")
  returnsOneOrNull()
}
```

The generated API, Feign, controller, and service interface do not accept `currentTodoId`:

```kotlin
fun findCurrentTodo(): TodoReportDto?
```

The generated service implementation resolves the value on the server:

```kotlin
override fun findCurrentTodo(): TodoReportDto? {
  val params = mapOf(
    "currentTodoId" to bean<CurrentTodoIdResolver>().resolve("currentTodoId"),
  )
  return executor.queryOneOrNull(FIND_CURRENT_TODO_SQL, params) { row ->
    TodoReportDto(
      id = row.getRequired<Int>("id"),
      description = row.getRequired<String>("description")
    )
  }
}
```

This prevents clients from changing server-owned filters by sending a query parameter or request field with the same name.

## Transactions

Configure the data source and transaction manager once per API:

```kotlin
sqlApi("TodoReportApi", "/api/todo-report") {
  database {
    dataSource("dataSource")
    transactionManager("transactionManager")
  }
}
```

Set an API-level transaction policy:

```kotlin
sqlApi("TodoCommandApi", "/api/todo-command") {
  transactional()
}
```

Override it per query or command:

```kotlin
query("searchTodos", "/search") {
  transactional(readOnly = true)
}

command("rebuildIndex", "/rebuild-index") {
  transactional(enabled = false)
}
```

Queries do not generate a transaction annotation unless an API-level or query-level transaction policy is configured. Commands default to read-write transactions.

## Validation Rules

The DSL validates common mismatches before writing generated files:

- Every SQL API query must select at least one aliased column.
- Declared columns must exist in the `SELECT ... AS alias` list.
- Declared parameters must be used by SQL.
- Path parameters must appear in the endpoint path.
- Query `bodyParam` is rejected.
- Paged queries must use the configured page size and offset parameters in the main SQL.
- Paged query count SQL must select the configured count column.
- Main SQL and count SQL share the same declared parameter set, including context parameters.

### Custom AST validation

Codegen projects can register validation rules for project-specific SQL policies. Each rule receives the original SQL and
the JSqlParser 5.0 `Statement` after built-in metadata extraction. Rules apply to main query SQL, page count SQL, and
`INSERT`, `UPDATE`, and `DELETE` commands.

```kotlin
class TodoSqlApiCodegen : SqlApiDslCodegen() {
  override val sqlValidationRules = listOf(
    SqlValidationRule { _, statement ->
      if (statement is Delete && statement.where == null) {
        listOf("DELETE statements must declare a WHERE clause")
      } else {
        emptyList()
      }
    },
  )

  override fun codegen() {
    // SQL API declarations
  }
}
```

Rules run synchronously in declaration order and findings are reported together with the API, function, and SQL role.
The JSqlParser AST is mutable, but validation rules must treat it as read-only. Rules inspecting nested CTEs, set
operations, or expression subqueries are responsible for traversing those nodes, typically with JSqlParser visitors.
The original SQL should be used for comment-based exemption markers because comments and named parameters are not
guaranteed to round-trip through the parsed AST.

## See Also

- [Web & REST APIs](web-rest.md)
- [DSL Properties](../reference/dsl-properties.md)
- [DSL-Based Tutorial](../tutorials/dsl-based.md)
