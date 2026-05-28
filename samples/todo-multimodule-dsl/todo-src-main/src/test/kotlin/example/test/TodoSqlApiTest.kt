package example.test

import example.api
import example.api.TodoApi
import example.sqlapi.api.TodoReportApi
import example.sqlapi.dto.SearchTodosReq
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import zygarde.codegen.data.dto.CreateTodoReq

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class TodoSqlApiTest {
  @Test
  fun `should search todos with generated SQL API`() {
    val todoApi = api<TodoApi>()
    val todoReportApi = api<TodoReportApi>()

    val first = todoApi.createTodo(CreateTodoReq("write SQL API"))
    todoApi.createTodo(CreateTodoReq("write GraphQL API"))

    todoReportApi.searchTodos(SearchTodosReq("SQL")).also {
      it.shouldHaveSize(1)
      it.single().id shouldBe first.id
      it.single().description shouldBe "write SQL API"
    }
  }
}
