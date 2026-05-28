package example.test

import example.ApiHelper
import example.api.TodoApi
import example.sqlapi.api.TodoCommandApi
import example.sqlapi.api.TodoReportApi
import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.DeleteTodoBySqlReq
import example.sqlapi.dto.FindTodoReq
import example.sqlapi.dto.PageTodosReq
import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.UpdateTodoBySqlReq
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.core.di.DiServiceContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class TodoSqlApiTest {
  @Test
  fun `should search todos with generated SQL API`() {
    val todoApi = feign<TodoApi>()
    val todoReportApi = feign<TodoReportApi>()

    val first = todoApi.createTodo(CreateTodoReq("write SQL API"))
    todoApi.createTodo(CreateTodoReq("write GraphQL API"))

    todoReportApi.searchTodos(SearchTodosReq("SQL")).also {
      it.shouldHaveSize(1)
      it.single().id shouldBe first.id
      it.single().description shouldBe "write SQL API"
    }
  }

  @Test
  fun `should find todo with generated single row SQL API`() {
    val todoApi = feign<TodoApi>()
    val todoReportApi = feign<TodoReportApi>()

    val todo = todoApi.createTodo(CreateTodoReq("find SQL API"))

    todoReportApi.findTodo(FindTodoReq(todo.id)).also {
      it?.id shouldBe todo.id
      it?.description shouldBe "find SQL API"
    }
    todoReportApi.findTodo(FindTodoReq(-1)) shouldBe null
  }

  @Test
  fun `should page todos with generated SQL API`() {
    val todoApi = feign<TodoApi>()
    val todoReportApi = feign<TodoReportApi>()

    todoApi.createTodo(CreateTodoReq("page SQL API 1"))
    todoApi.createTodo(CreateTodoReq("page SQL API 2"))
    todoApi.createTodo(CreateTodoReq("page SQL API 3"))

    todoReportApi.pageTodos(PageTodosReq(keyword = "page SQL", atPage = 1, pageSize = 2)).also {
      it.atPage shouldBe 1
      it.totalPages shouldBe 2
      it.totalCount shouldBe 3
      it.items.shouldHaveSize(1)
      it.items.single().description shouldBe "page SQL API 3"
    }
  }

  @Test
  fun `should execute generated SQL command API`() {
    val todoCommandApi = feign<TodoCommandApi>()
    val todoReportApi = feign<TodoReportApi>()

    val created = todoCommandApi.createTodo(CreateTodoBySqlReq("command SQL API"))

    todoReportApi.findTodo(FindTodoReq(created.id))?.description shouldBe "command SQL API"
    todoCommandApi.updateTodo(UpdateTodoBySqlReq(id = created.id, description = "updated command SQL API")) shouldBe 1
    todoReportApi.findTodo(FindTodoReq(created.id))?.description shouldBe "updated command SQL API"
    todoCommandApi.deleteTodo(DeleteTodoBySqlReq(created.id))
    todoReportApi.findTodo(FindTodoReq(created.id)) shouldBe null
  }

  private inline fun <reified T : Any> feign(): T {
    val kClass = ApiHelper.apiToFeignClientClassMapping[T::class]
    return kClass?.java?.let { DiServiceContext.ctx.getBean(it) as T }
      ?: throw IllegalArgumentException("no FeignClient found for ${T::class.java.simpleName}")
  }
}
