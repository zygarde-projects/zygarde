package example

import example.codegen.data.dao.TodoDao
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.SearchTodoReq
import zygarde.codegen.data.dto.TodoDto
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.codegen.dsl.webmvc.DslApiFunction
import zygarde.codegen.dsl.webmvc.WebMvcDslCodegen

class TodoApiCodegen : WebMvcDslCodegen() {
  override fun codegen() {
    api("TodoApi", "/api/todo") {
      fun DslApiFunction.todoIdPathVariable() {
        pathVariable<Int>("todoId")
      }

      get("getTodoList", "") {
        resCollection<TodoDto>()
      }
      postPage<SearchTodoReq, TodoDto>("searchTodos", "search") {
      }
      get("getTodo", "{todoId}") {
        todoIdPathVariable()
        res<TodoDto>()
      }
      post("createTodo", "") {
        req<CreateTodoReq>()
        res<TodoDto>()
      }
      put("updateTodo", "{todoId}") {
        todoIdPathVariable()
        req<UpdateTodoReq>()
        res<TodoDto>()
      }
      delete("deleteTodo", "{todoId}") {
        todoIdPathVariable()
      }

      crudServiceImpl<Todo, Int>("TodoApiService") {
        dao<TodoDao>("todoDao")
        dtoBuilder<TodoDto>("TodoDtoBuilder")
        applyExtensions("TodoApplyValueExtensions")
        list("getTodoList")
        page<SearchTodoReq>("searchTodos")
        get("getTodo", idParam = "todoId")
        create<CreateTodoReq>("createTodo")
        update<UpdateTodoReq>("updateTodo", idParam = "todoId")
        delete("deleteTodo", idParam = "todoId")
      }
    }

    api("TodoApi2", "api/todo2") {
      get("getTodoList", "") {
        resCollection<TodoDto>()
        serviceName = "TodoApiService" // we can share TodoApiService that generated from TodoApi Spec
      }
    }
  }
}
