package example.graphql.service.impl

import example.codegen.data.dao.TodoDao
import example.codegen.data.dao.search
import example.codegen.entity.search.description
import example.codegen.entity.search.id
import example.graphql.TodoFilter
import example.graphql.service.TodoGraphQlService
import example.service.TodoApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.TodoDto
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.codegen.model.extensions.TodoDtoBuilder

@Service
class TodoGraphQlServiceImpl(
  @Autowired private val todoApiService: TodoApiService,
  @Autowired private val todoDao: TodoDao,
) : TodoGraphQlService {
  override fun todos(filter: TodoFilter?): Collection<TodoDto> {
    return todoDao.search {
      id() eq filter?.idEq
      description() contains filter?.descriptionContains
    }.map(TodoDtoBuilder::build)
  }

  override fun todo(id: Int): TodoDto? {
    return todoDao.findById(id).orElse(null)?.let(TodoDtoBuilder::build)
  }

  override fun todosByIds(ids: Collection<Int>): Collection<TodoDto> {
    return todoDao.search {
      id() inList ids
    }.map(TodoDtoBuilder::build)
  }

  override fun createTodo(input: CreateTodoReq): TodoDto {
    return todoApiService.createTodo(input) {}
  }

  override fun updateTodo(id: Int, input: UpdateTodoReq): TodoDto {
    return todoApiService.updateTodo(id, input)
  }

  override fun deleteTodo(id: Int): Boolean {
    todoApiService.deleteTodo(id)
    return true
  }
}
