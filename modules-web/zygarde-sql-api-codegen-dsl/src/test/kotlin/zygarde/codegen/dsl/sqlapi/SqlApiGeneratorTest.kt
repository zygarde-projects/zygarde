package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

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
