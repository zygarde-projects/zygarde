package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod

class SqlApiGeneratorTest {
  private fun api(): SqlApiToGenerateVo {
    val codegen = SqlApiDslCodegenTest()
    codegen.codegen()
    return codegen.apisToGenerate.single()
  }

  @Test
  fun `should generate DTO schema nullability and web API contract`() {
    val result = SqlApiGenerator(listOf(api())).generate()

    result.dtoFileSpecs.first { it.name == "SearchTodosReq" }.toString().also {
      it shouldContain "public var keyword: String? = null"
      it shouldContain """description="Search keyword""""
      it shouldContain "requiredMode=Schema.RequiredMode.NOT_REQUIRED"
    }
    result.dtoFileSpecs.first { it.name == "TodoReportDto" }.toString().also {
      it shouldContain "public var id: Int"
      it shouldContain "public var description: String"
      it shouldContain "public var createdBy: String? = null"
      it shouldContain "requiredMode=Schema.RequiredMode.REQUIRED"
    }
    result.webApiGenerateResult.apiInterfaces.single().toString().also {
      it shouldContain "public fun searchTodos(req: SearchTodosReq): Collection<TodoReportDto>"
    }
  }

  @Test
  fun `should generate service implementation with datasource sql params and row mapper`() {
    val result = SqlApiGenerator(listOf(api())).generate()

    result.serviceImplFileSpecs.single().toString().also {
      it shouldContain "private val dataSource: DataSource"
      it shouldContain "private val executor: ZygardeSqlExecutor = ZygardeSqlExecutor(dataSource)"
      it shouldContain "private const val SEARCH_TODOS_SQL"
      it shouldContain "\"keyword\" to req.keyword"
      it shouldContain "id = row.getRequired<Int>(\"id\")"
      it shouldContain "createdBy = row.getNullable<String>(\"createdBy\")"
    }
  }

  @Test
  fun `should generate single row query contract and service implementation`() {
    val api = SqlApiToGenerateVo(
      config = SqlApiDslCodegenConfig(
        dtoPackage = "example.dto",
        apiInterfacePackage = "example.api",
        controllerPackage = "example.controller",
        serviceInterfacePackage = "example.service",
        serviceImplPackage = "example.service.impl",
      ),
      apiName = "TodoReportApi",
      basePath = "/api/todo-report",
      queries = listOf(
        SqlQueryToGenerateVo(
          functionName = "findTodo",
          path = "/find",
          sql = "select id as id from todo where id = :id",
          requestName = "FindTodoReq",
          responseName = "TodoReportDto",
          params = listOf(SqlApiField("id", Int::class.asTypeName())),
          columns = listOf(SqlApiField("id", Int::class.asTypeName())),
          resultShape = SqlQueryResultShape.ONE_NULLABLE,
        )
      ),
    )

    val result = SqlApiGenerator(listOf(api)).generate()

    result.webApiGenerateResult.apiInterfaces.single().toString().also {
      it shouldContain "public fun findTodo(req: FindTodoReq): TodoReportDto?"
      it shouldNotContain "Collection<TodoReportDto>"
    }
    result.serviceImplFileSpecs.single().toString().also {
      it shouldContain "override fun findTodo(req: FindTodoReq): TodoReportDto?"
      it shouldContain "return executor.queryOneOrNull(FIND_TODO_SQL, params)"
    }
  }

  @Test
  fun `should generate page query contract and service implementation`() {
    val api = SqlApiToGenerateVo(
      config = SqlApiDslCodegenConfig(
        dtoPackage = "example.dto",
        apiInterfacePackage = "example.api",
        controllerPackage = "example.controller",
        serviceInterfacePackage = "example.service",
        serviceImplPackage = "example.service.impl",
      ),
      apiName = "TodoReportApi",
      basePath = "/api/todo-report",
      queries = listOf(
        SqlQueryToGenerateVo(
          functionName = "pageTodos",
          path = "/page",
          sql = "select id as id from todo limit :pageSize offset :offset",
          requestName = "PageTodosReq",
          responseName = "TodoReportDto",
          params = listOf(
            SqlApiField("pageSize", Int::class.asTypeName()),
            SqlApiField("atPage", Int::class.asTypeName()),
          ),
          columns = listOf(SqlApiField("id", Int::class.asTypeName())),
          resultShape = SqlQueryResultShape.PAGE,
          page = SqlApiPageToGenerateVo(
            countSql = "select count(*) as totalCount from todo",
            countColumnName = "totalCount",
            pageParamName = "atPage",
            pageSizeParamName = "pageSize",
            offsetParamName = "offset",
          ),
        )
      ),
    )

    val result = SqlApiGenerator(listOf(api)).generate()

    result.webApiGenerateResult.apiInterfaces.single().toString().also {
      it shouldContain "public fun pageTodos(req: PageTodosReq): PageDto<TodoReportDto>"
    }
    result.serviceImplFileSpecs.single().toString().also {
      it shouldContain "private const val PAGE_TODOS_COUNT_SQL"
      it shouldContain "\"offset\" to (req.atPage * req.pageSize)"
      it shouldContain "val items = executor.query(PAGE_TODOS_SQL, params)"
      it shouldContain "val totalCount = executor.queryOne(PAGE_TODOS_COUNT_SQL, params)"
      it shouldContain "return PageDto(req.atPage, totalPages, items, totalCount)"
    }
  }

  @Test
  fun `should generate command contracts and service implementations`() {
    val api = SqlApiToGenerateVo(
      config = SqlApiDslCodegenConfig(
        dtoPackage = "example.dto",
        apiInterfacePackage = "example.api",
        controllerPackage = "example.controller",
        serviceInterfacePackage = "example.service",
        serviceImplPackage = "example.service.impl",
      ),
      apiName = "TodoCommandApi",
      basePath = "/api/todo-command",
      queries = emptyList(),
      commands = listOf(
        SqlCommandToGenerateVo(
          functionName = "createTodo",
          path = "/create",
          sql = "insert into todo(description, check_times) values (:description, 0)",
          method = RequestMethod.POST,
          requestName = "CreateTodoBySqlReq",
          params = listOf(SqlApiField("description", String::class.asTypeName())),
          resultShape = SqlCommandResultShape.GENERATED_KEY,
          generatedKey = SqlGeneratedKeyToGenerateVo(
            responseName = "CreatedTodoKeyDto",
            field = SqlApiField("id", Int::class.asTypeName()),
            keyColumnName = "id",
          ),
        ),
        SqlCommandToGenerateVo(
          functionName = "updateTodo",
          path = "/update",
          sql = "update todo set description = :description where id = :id",
          method = RequestMethod.PUT,
          requestName = "UpdateTodoBySqlReq",
          params = listOf(
            SqlApiField("description", String::class.asTypeName()),
            SqlApiField("id", Int::class.asTypeName()),
          ),
        ),
        SqlCommandToGenerateVo(
          functionName = "deleteTodo",
          path = "/delete",
          sql = "delete from todo where id = :id",
          method = RequestMethod.DELETE,
          requestName = "DeleteTodoBySqlReq",
          params = listOf(SqlApiField("id", Int::class.asTypeName())),
          resultShape = SqlCommandResultShape.NO_CONTENT,
        )
      ),
    )

    val result = SqlApiGenerator(listOf(api)).generate()

    result.dtoFileSpecs.map { it.name } shouldBe listOf(
      "CreateTodoBySqlReq",
      "CreatedTodoKeyDto",
      "UpdateTodoBySqlReq",
      "DeleteTodoBySqlReq",
    )
    result.webApiGenerateResult.apiInterfaces.single().toString().also {
      it shouldContain "public fun createTodo(req: CreateTodoBySqlReq): CreatedTodoKeyDto"
      it shouldContain "public fun updateTodo(req: UpdateTodoBySqlReq): Int"
      it shouldContain "public fun deleteTodo(req: DeleteTodoBySqlReq)"
    }
    result.serviceImplFileSpecs.single().toString().also {
      it shouldContain "private const val CREATE_TODO_SQL"
      it shouldContain "val key = executor.insertAndReturnKey<Int>(CREATE_TODO_SQL, params, \"id\")"
      it shouldContain "return CreatedTodoKeyDto(id = key)"
      it shouldContain "return executor.execute(UPDATE_TODO_SQL, params)"
      it shouldContain "executor.execute(DELETE_TODO_SQL, params)"
    }
  }

  @Test
  fun `should generate path query and body parameter contracts`() {
    val api = SqlApiToGenerateVo(
      config = SqlApiDslCodegenConfig(
        dtoPackage = "example.dto",
        apiInterfacePackage = "example.api",
        controllerPackage = "example.controller",
        serviceInterfacePackage = "example.service",
        serviceImplPackage = "example.service.impl",
      ),
      apiName = "TodoCommandApi",
      basePath = "/api/todo-command",
      queries = listOf(
        SqlQueryToGenerateVo(
          functionName = "findTodo",
          path = "/find/{id}",
          sql = "select id as id from todo where id = :id and (:keyword is null or description like :keyword)",
          requestName = "FindTodoReq",
          responseName = "TodoReportDto",
          params = listOf(
            SqlApiField("id", Int::class.asTypeName(), source = SqlApiParamSource.PATH),
            SqlApiField("keyword", String::class.asTypeName().copy(nullable = true), source = SqlApiParamSource.QUERY),
          ),
          columns = listOf(SqlApiField("id", Int::class.asTypeName())),
          resultShape = SqlQueryResultShape.ONE_NULLABLE,
        )
      ),
      commands = listOf(
        SqlCommandToGenerateVo(
          functionName = "updateTodo",
          path = "/update/{id}",
          sql = "update todo set description = :description where id = :id",
          method = RequestMethod.PUT,
          requestName = "UpdateTodoBySqlReq",
          params = listOf(
            SqlApiField("description", String::class.asTypeName(), source = SqlApiParamSource.BODY),
            SqlApiField("id", Int::class.asTypeName(), source = SqlApiParamSource.PATH),
          ),
        )
      ),
    )

    val result = SqlApiGenerator(listOf(api)).generate()

    result.dtoFileSpecs.map { it.name } shouldBe listOf("TodoReportDto", "UpdateTodoBySqlReq")
    result.webApiGenerateResult.apiInterfaces.single().toString().also {
      it shouldContain "public fun findTodo(id: Int, keyword: String?): TodoReportDto?"
      it shouldContain "public fun updateTodo(id: Int, req: UpdateTodoBySqlReq): Int"
      it shouldNotContain "FindTodoReq"
    }
    result.webApiGenerateResult.feignApiInterfaces.single().toString().also {
      it shouldContain "@PathVariable(value=\"id\")"
      it shouldContain "@RequestParam(value=\"keyword\""
      it shouldContain "required=false"
      it shouldContain "@RequestBody"
    }
    result.serviceImplFileSpecs.single().toString().also {
      it shouldContain "override fun findTodo(id: Int, keyword: String?): TodoReportDto?"
      it shouldContain "\"id\" to id"
      it shouldContain "\"keyword\" to keyword"
      it shouldContain "override fun updateTodo(id: Int, req: UpdateTodoBySqlReq): Int"
      it shouldContain "\"description\" to req.description"
    }
  }

  @Test
  fun `should generate no request DTO or req parameter for query without params`() {
    val api = SqlApiToGenerateVo(
      config = SqlApiDslCodegenConfig(
        dtoPackage = "example.dto",
        apiInterfacePackage = "example.api",
        controllerPackage = "example.controller",
        serviceInterfacePackage = "example.service",
        serviceImplPackage = "example.service.impl",
      ),
      apiName = "TodoReportApi",
      basePath = "/api/todo-report",
      queries = listOf(
        SqlQueryToGenerateVo(
          functionName = "listTodos",
          path = "/list",
          sql = "select id as id from todo",
          requestName = "ListTodosReq",
          responseName = "TodoReportDto",
          params = emptyList(),
          columns = listOf(SqlApiField("id", Int::class.asTypeName())),
        )
      ),
    )

    val result = SqlApiGenerator(listOf(api)).generate()

    result.dtoFileSpecs.map { it.name }.shouldNotContain("ListTodosReq")
    result.webApiGenerateResult.apiInterfaces.single().toString().also {
      it shouldContain "public fun listTodos(): Collection<TodoReportDto>"
      it shouldNotContain "ListTodosReq"
    }
    result.serviceImplFileSpecs.single().toString().also {
      it shouldContain "override fun listTodos(): Collection<TodoReportDto>"
      it shouldContain "val params = emptyMap<String, Any?>()"
      it shouldNotContain "req."
    }
  }

  @Test
  fun `should reject conflicting DTO definitions with same name`() {
    val api = SqlApiToGenerateVo(
      config = SqlApiDslCodegenConfig(
        dtoPackage = "example.dto",
        apiInterfacePackage = "example.api",
        controllerPackage = "example.controller",
        serviceInterfacePackage = "example.service",
        serviceImplPackage = "example.service.impl",
      ),
      apiName = "TodoReportApi",
      basePath = "/api/todo-report",
      queries = listOf(
        SqlQueryToGenerateVo(
          functionName = "searchByKeyword",
          path = "/search-by-keyword",
          sql = "select id as id from todo where keyword = :keyword",
          requestName = "SearchReq",
          responseName = "TodoReportDto",
          params = listOf(SqlApiField("keyword", String::class.asTypeName())),
          columns = listOf(SqlApiField("id", Int::class.asTypeName())),
        ),
        SqlQueryToGenerateVo(
          functionName = "searchByOwner",
          path = "/search-by-owner",
          sql = "select id as id from todo where owner_id = :ownerId",
          requestName = "SearchReq",
          responseName = "TodoReportDto",
          params = listOf(SqlApiField("ownerId", Int::class.asTypeName())),
          columns = listOf(SqlApiField("id", Int::class.asTypeName())),
        )
      ),
    )

    val error = shouldThrow<IllegalArgumentException> {
      SqlApiGenerator(listOf(api)).generate()
    }

    error.message shouldBe "SQL API DTO 'SearchReq' is declared with conflicting fields"
  }
}
