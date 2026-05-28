package zygarde.codegen.dsl.sqlapi

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
}
