package zygarde.samples.todo

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.boot.test.web.client.postForObject
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.annotation.DirtiesContext
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.PagingRequest
import zygarde.data.api.SortField
import zygarde.samples.todo.generated.dao.TodoDao
import zygarde.samples.todo.generated.dao.search
import zygarde.samples.todo.generated.dao.searchCount
import zygarde.samples.todo.generated.dao.searchOne
import zygarde.samples.todo.generated.dao.searchOneOrThrow
import zygarde.samples.todo.generated.dao.searchPage
import zygarde.samples.todo.generated.dto.CreateTodoReq
import zygarde.samples.todo.generated.dto.TodoDto
import zygarde.samples.todo.generated.dto.UpdateTodoReq
import zygarde.samples.todo.generated.dto.applyFromCreateTodoReq
import zygarde.samples.todo.generated.dto.applyFromUpdateTodoReq
import zygarde.samples.todo.generated.dto.toTodoDto
import zygarde.samples.todo.generated.search.description
import zygarde.samples.todo.generated.search.id
import zygarde.samples.todo.generated.search.priority
import io.kotest.assertions.throwables.shouldThrow

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = [TodoApplication::class],
  properties = ["zygarde.api.static-option-api.path=/api/staticOptions"]
)
@DirtiesContext
class TodoApiIntegrationTest {
  @Autowired
  lateinit var restTemplate: TestRestTemplate

  @Autowired
  lateinit var todoDao: TodoDao

  @Autowired
  lateinit var todoService: MyTodoService

  @BeforeEach
  fun setUp() {
    todoDao.deleteAll()
  }

  @Test
  fun `todo API should create list update and expose static options`() {
    val created = restTemplate.postForObject<TodoDto>(
      "/api/todo",
      CreateTodoReq(description = "write tests", priority = 1)
    )!!

    created.description shouldBe "write tests"
    created.priority shouldBe 1
    todoService.searchByDescription("write tests") shouldHaveSize 1

    val listed = restTemplate.getForObject<Array<TodoDto>>("/api/todo")!!.toList()
    listed shouldHaveSize 1
    listed.first().id shouldBe created.id

    restTemplate.exchange(
      "/api/todo/${created.id}",
      HttpMethod.PUT,
      HttpEntity(UpdateTodoReq(description = "ship coverage", priority = 2)),
      TodoDto::class.java
    ).body!!.also {
      it.id shouldBe created.id
      it.description shouldBe "ship coverage"
      it.priority shouldBe 2
    }

    val statuses = restTemplate.getForObject<Array<Map<String, Any>>>("/api/staticOptions/todoStatus")!!.toList()
    statuses.map { it["key"] } shouldBe listOf("DOING", "DONE")
  }

  @Test
  fun `generated DTO extensions should apply nullable create request and update request`() {
    val todo = Todo(description = "original", priority = 1)

    todo.applyFromCreateTodoReq(CreateTodoReq(description = null, priority = 3))
    todo.description shouldBe "original"
    todo.priority shouldBe 3

    todo.applyFromUpdateTodoReq(UpdateTodoReq(description = "updated", priority = 4))
    todo.description shouldBe "updated"
    todo.priority shouldBe 4

    val saved = todoDao.saveAndFlush(todo)
    saved.toTodoDto().also {
      it.id shouldBe saved.id
      it.description shouldBe "updated"
      it.priority shouldBe 4
    }
  }

  @Test
  fun `generated Todo DAO search overloads should query count page and throw`() {
    todoDao.saveAll(
      listOf(
        Todo(description = "alpha", priority = 1),
        Todo(description = "beta", priority = 2),
        Todo(description = "alpha high", priority = 3),
      )
    )

    todoDao.search { description() contains "alpha" } shouldHaveSize 2
    todoDao.search(listOf(SortField(field = "priority"))) { id() gt 0 }.map { it.priority } shouldBe listOf(1, 2, 3)
    todoDao.search({ priority() gte 1 }, limit = 2) shouldHaveSize 2
    todoDao.searchCount { description() startsWith "alpha" } shouldBe 2L
    todoDao.searchOne { description() eq "beta" }!!.priority shouldBe 2
    todoDao.searchOneOrThrow(ApiErrorCode.NOT_FOUND) { description() eq "alpha" }.description shouldBe "alpha"
    shouldThrow<BusinessException> {
      todoDao.searchOneOrThrow(ApiErrorCode.NOT_FOUND) { description() eq "missing" }
    }.code shouldBe ApiErrorCode.NOT_FOUND

    val page = todoDao.searchPage(PagingAndSortingRequest().also { it.paging = PagingRequest(page = 1, pageSize = 2) }) {
      priority() gt 0
    }
    page.content shouldHaveSize 2
  }
}
