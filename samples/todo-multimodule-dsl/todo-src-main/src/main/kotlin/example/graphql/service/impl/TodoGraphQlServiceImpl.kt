package example.graphql.service.impl

import example.codegen.data.dao.TodoDao
import example.codegen.data.dao.search
import example.codegen.entity.search.description
import example.codegen.entity.search.id
import example.graphql.TodoFilter
import example.graphql.service.TodoGraphQlService
import example.graphql.service.TodoGraphQlSource
import example.graphql.service.TodoGraphQlSourceAssembler
import example.service.TodoApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.UpdateTodoReq

@Service
class TodoGraphQlServiceImpl(
  @Autowired private val todoApiService: TodoApiService,
  @Autowired private val todoDao: TodoDao,
) : TodoGraphQlService {
  private val todoGraphQlSourceAssembler = TodoGraphQlSourceAssembler()

  override fun todos(filter: TodoFilter?): Collection<TodoGraphQlSource> {
    return todoGraphQlSourceAssembler.buildAll(
      todoDao.search {
        id() eq filter?.idEq
        id() inList filter?.idsIn
        description() contains filter?.descriptionContains
      }
    )
  }

  override fun todo(id: Int): TodoGraphQlSource? {
    return todoDao.findById(id).orElse(null)?.let(todoGraphQlSourceAssembler::build)
  }

  override fun todosByIds(ids: Collection<Int>): Collection<TodoGraphQlSource> {
    return todoGraphQlSourceAssembler.buildAll(
      todoDao.search {
        id() inList ids
      }
    )
  }

  override fun createTodo(input: CreateTodoReq): TodoGraphQlSource {
    val created = todoApiService.createTodo(input)
    return requireNotNull(todo(created.id)) { "Created todo '${created.id}' was not found" }
  }

  override fun updateTodo(id: Int, input: UpdateTodoReq): TodoGraphQlSource {
    todoApiService.updateTodo(id, input)
    return requireNotNull(todo(id)) { "Updated todo '$id' was not found" }
  }

  override fun deleteTodo(id: Int): Boolean {
    todoApiService.deleteTodo(id)
    return true
  }
}
