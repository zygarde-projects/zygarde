package example.test

import example.api
import example.api.TodoApi
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.SearchTodoReq
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.data.api.PagingRequest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class TodoApiTest {
  @Test
  fun `todo crud test`() {
    val todoApi = api<TodoApi>()

    todoApi.getTodoList().size shouldBe 0

    val todo = todoApi.createTodo(CreateTodoReq("my first todo"))

    todoApi.getTodo(todo.id).also {
      it.description shouldBe "my first todo"
    }

    todoApi.updateTodo(todo.id, UpdateTodoReq("not my first todo"))

    todoApi.getTodo(todo.id).also {
      it.description shouldBe "not my first todo"
    }

    todoApi.getTodoList().size shouldBe 1

    todoApi.searchTodos(
      SearchTodoReq().apply {
        paging = PagingRequest(page = 1, pageSize = 1)
      }
    ).also {
      it.atPage shouldBe 1
      it.totalPages shouldBe 1
      it.totalCount shouldBe 1
      it.items.size shouldBe 1
      it.items[0].`file`?.name shouldBe "File todo-file"
    }

    todoApi.deleteTodo(todo.id)

    todoApi.getTodoList().size shouldBe 0
  }
}
