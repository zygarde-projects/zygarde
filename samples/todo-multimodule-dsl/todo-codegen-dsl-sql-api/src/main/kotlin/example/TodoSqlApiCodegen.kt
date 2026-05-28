package example

import zygarde.codegen.dsl.sqlapi.SqlApiDslCodegen

class TodoSqlApiCodegen : SqlApiDslCodegen() {
  override fun codegen() {
    sqlApi("TodoReportApi", "/api/todo-report") {
      query("searchTodos", "/search") {
        sql(
          """
          select t.id as id, t.description as description
          from todo t
          where (:keyword is null or t.description like concat('%', :keyword, '%'))
          order by t.id
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
}
