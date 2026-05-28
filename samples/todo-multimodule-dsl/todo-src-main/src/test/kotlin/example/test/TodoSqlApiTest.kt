package example.test

import example.ApiHelper
import example.api.TodoApi
import example.sqlapi.api.TodoCommandApi
import example.sqlapi.api.TodoReportApi
import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.UpdateTodoBySqlReq
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.core.di.DiServiceContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class TodoSqlApiTest {
  @Autowired
  lateinit var restTemplate: TestRestTemplate

  @Test
  fun `should search todos with generated SQL API`() {
    val todoApi = feign<TodoApi>()
    val todoReportApi = feign<TodoReportApi>()

    val first = todoApi.createTodo(CreateTodoReq("write SQL API"))
    todoApi.createTodo(CreateTodoReq("write GraphQL API"))

    todoReportApi.searchTodos("SQL").also {
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

    todoReportApi.findTodo(todo.id).also {
      it?.id shouldBe todo.id
      it?.description shouldBe "find SQL API"
    }
    todoReportApi.findTodo(-1) shouldBe null
  }

  @Test
  fun `should find todos by ids with generated SQL API collection parameter`() {
    val todoApi = feign<TodoApi>()
    val todoReportApi = feign<TodoReportApi>()

    val first = todoApi.createTodo(CreateTodoReq("find SQL API by ids 1"))
    todoApi.createTodo(CreateTodoReq("find SQL API by ids ignored"))
    val third = todoApi.createTodo(CreateTodoReq("find SQL API by ids 3"))

    todoReportApi.findTodosByIds(listOf(first.id, third.id)).also {
      it.shouldHaveSize(2)
      it.map { todo -> todo.id } shouldBe listOf(first.id, third.id)
    }
  }

  @Test
  fun `should page todos with generated SQL API`() {
    val todoApi = feign<TodoApi>()
    val todoReportApi = feign<TodoReportApi>()

    todoApi.createTodo(CreateTodoReq("page SQL API 1"))
    todoApi.createTodo(CreateTodoReq("page SQL API 2"))
    todoApi.createTodo(CreateTodoReq("page SQL API 3"))

    todoReportApi.pageTodos(keyword = "page SQL", atPage = 1, pageSize = 2).also {
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

    todoReportApi.findTodo(created.id)?.description shouldBe "command SQL API"
    todoCommandApi.updateTodo(created.id, UpdateTodoBySqlReq(description = "updated command SQL API")) shouldBe 1
    todoReportApi.findTodo(created.id)?.description shouldBe "updated command SQL API"
    todoCommandApi.deleteTodo(created.id)
    todoReportApi.findTodo(created.id) shouldBe null
  }

  @Test
  fun `should return generated SQL command HTTP statuses`() {
    val createResponse = restTemplate.postForEntity(
      "/api/todo-command/create",
      CreateTodoBySqlReq("command SQL API status"),
      CreatedTodoKeyDto::class.java,
    )

    createResponse.statusCode shouldBe HttpStatus.CREATED

    val deleteResponse = restTemplate.exchange(
      "/api/todo-command/delete/${createResponse.body?.id}",
      HttpMethod.DELETE,
      HttpEntity.EMPTY,
      Void::class.java,
    )

    deleteResponse.statusCode shouldBe HttpStatus.NO_CONTENT
  }

  private inline fun <reified T : Any> feign(): T {
    val kClass = ApiHelper.apiToFeignClientClassMapping[T::class]
    return kClass?.java?.let { DiServiceContext.ctx.getBean(it) as T }
      ?: throw IllegalArgumentException("no FeignClient found for ${T::class.java.simpleName}")
  }
}
