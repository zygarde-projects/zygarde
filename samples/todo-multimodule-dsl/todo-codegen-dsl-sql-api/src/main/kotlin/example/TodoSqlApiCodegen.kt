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
        queryParam<String?>("keyword", description = "Search keyword")
        column<Int>("id")
        column<String>("description")
        request("SearchTodosReq")
        response("TodoReportDto")
      }
      query("findTodo", "/find/{id}") {
        sql(
          """
          select t.id as id, t.description as description
          from todo t
          where t.id = :id
          """.trimIndent()
        )
        pathParam<Int>("id", description = "Todo id")
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
        queryParam<String?>("keyword", description = "Search keyword")
        queryParam<Int>("atPage", description = "Page index")
        queryParam<Int>("pageSize", description = "Page size")
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
        bodyParam<String>("description", description = "Todo description")
        request("CreateTodoBySqlReq")
        returnsGeneratedKey<Int>("id", responseName = "CreatedTodoKeyDto")
      }
      command("updateTodo", "/update/{id}") {
        put()
        sql(
          """
          update todo
          set description = :description
          where id = :id
          """.trimIndent()
        )
        pathParam<Int>("id", description = "Todo id")
        bodyParam<String>("description", description = "Todo description")
        request("UpdateTodoBySqlReq")
        returnsAffectedRows()
      }
      command("deleteTodo", "/delete/{id}") {
        delete()
        sql(
          """
          delete from todo
          where id = :id
          """.trimIndent()
        )
        pathParam<Int>("id", description = "Todo id")
        request("DeleteTodoBySqlReq")
        returnsNoContent()
      }
    }
  }
}
