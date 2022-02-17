package example.test

import example.api
import example.api.TodoApi
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.UpdateTodoReq

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

    todoApi.deleteTodo(todo.id)

    todoApi.getTodoList().size shouldBe 0
  }
}
