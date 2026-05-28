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
      query("findTodo", "/find") {
        sql(
          """
          select t.id as id, t.description as description
          from todo t
          where t.id = :id
          """.trimIndent()
        )
        param<Int>("id", description = "Todo id")
        column<Int>("id")
        column<String>("description")
        request("FindTodoReq")
        response("TodoReportDto")
        returnsOneOrNull()
      }
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
        param<String?>("keyword", description = "Search keyword")
        param<Int>("atPage", description = "Page index")
        param<Int>("pageSize", description = "Page size")
        column<Int>("id")
        column<String>("description")
        request("PageTodosReq")
        response("TodoReportDto")
      }
    }
    sqlApi("TodoCommandApi", "/api/todo-command") {
      command("createTodo", "/create") {
        sql(
          """
          insert into todo(description, check_times)
          values (:description, 0)
          """.trimIndent()
        )
        param<String>("description", description = "Todo description")
        request("CreateTodoBySqlReq")
        returnsGeneratedKey<Int>("id", responseName = "CreatedTodoKeyDto")
      }
      command("updateTodo", "/update") {
        put()
        sql(
          """
          update todo
          set description = :description
          where id = :id
          """.trimIndent()
        )
        param<Int>("id", description = "Todo id")
        param<String>("description", description = "Todo description")
        request("UpdateTodoBySqlReq")
        returnsAffectedRows()
      }
      command("deleteTodo", "/delete") {
        delete()
        sql(
          """
          delete from todo
          where id = :id
          """.trimIndent()
        )
        param<Int>("id", description = "Todo id")
        request("DeleteTodoBySqlReq")
        returnsNoContent()
      }
    }
  }
}
