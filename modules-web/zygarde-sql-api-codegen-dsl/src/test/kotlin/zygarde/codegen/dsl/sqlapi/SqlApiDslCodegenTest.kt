package zygarde.codegen.dsl.sqlapi

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class SqlApiDslCodegenTest : SqlApiDslCodegen() {
  override fun codegen() {
    sqlApi("TodoReportApi", "/api/todo-report") {
      query("searchTodos", "/search") {
        sql(
          """
          select t.id as id, t.description as description, t.created_by as createdBy
          from todo t
          where (:keyword is null or t.description like concat('%', :keyword, '%'))
          """.trimIndent()
        )
        param<String?>("keyword", description = "Search keyword")
        column<Int>("id")
        column<String>("description")
        request("SearchTodosReq")
        response("TodoReportDto")
      }
    }
  }

  @Test
  fun `should collect SQL API metadata with nullable params and default columns`() {
    codegen()

    val query = apisToGenerate.single().queries.single()

    query.params.single().also {
      it.name shouldBe "keyword"
      it.type.isNullable shouldBe true
      it.description shouldBe "Search keyword"
    }
    query.columns.shouldHaveSize(3)
    query.columns[0].name shouldBe "id"
    query.columns[0].type.isNullable shouldBe false
    query.columns[1].name shouldBe "description"
    query.columns[1].type.isNullable shouldBe false
    query.columns[2].name shouldBe "createdBy"
    query.columns[2].type.isNullable shouldBe true
  }

  @Test
  fun `should reject declared params that SQL does not use`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo where id = :id")
    query.param<Int>("id")
    query.param<String>("unused")
    query.column<Int>("id")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message shouldBe "SQL query 'searchTodos' declares parameters that are not used by SQL: unused"
  }

  @Test
  fun `should reject declared columns that SQL does not select`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo")
    query.column<Int>("missing")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message shouldBe "SQL query 'searchTodos' declares columns that are not selected by SQL: missing"
  }

  @Test
  fun `should collect page metadata and default page params`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql(
      """
      select id as id
      from todo
      where (:keyword is null or description like :keyword)
      order by id
      limit :pageSize offset :offset
      """.trimIndent()
    )
    query.returnsPage(
      countSql = """
        select count(*) as totalCount
        from todo
        where (:keyword is null or description like :keyword)
      """.trimIndent()
    )
    query.param<String?>("keyword")
    query.column<Int>("id")

    val metadata = query.toSqlQueryToGenerateVo()

    metadata.resultShape shouldBe SqlQueryResultShape.PAGE
    metadata.params.map { it.name } shouldBe listOf("keyword", "pageSize", "atPage")
    metadata.params.first { it.name == "pageSize" }.type.isNullable shouldBe false
    metadata.params.first { it.name == "atPage" }.type.isNullable shouldBe false
  }

  @Test
  fun `should reject page SQL without offset parameter`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo limit :pageSize")
    query.returnsPage("select count(*) as totalCount from todo")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message.shouldContain("must use offset parameter")
  }
}
