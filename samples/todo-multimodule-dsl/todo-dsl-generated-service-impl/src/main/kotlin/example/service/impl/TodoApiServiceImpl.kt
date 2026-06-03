package example.service.`impl`

import example.Todo
import example.codegen.`data`.dao.TodoDao
import example.service.TodoApiService
import kotlin.Int
import kotlin.collections.Collection
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Service
import zygarde.`data`.api.PageDto
import zygarde.`data`.jpa.search.request.toSpringDataPageRequest
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.SearchTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq
import zygarde.codegen.model.extensions.TodoApplyValueExtensions.applyFrom
import zygarde.codegen.model.extensions.TodoDtoAssembler
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException

@Service
public class TodoApiServiceImpl(
  @Autowired
  private val todoDao: TodoDao,
  @Autowired
  private val todoDtoAssembler: TodoDtoAssembler,
) : TodoApiService {
  override fun getTodoList(): Collection<TodoDto> = todoDtoAssembler.buildAll(todoDao.findAll())

  override fun searchTodos(req: SearchTodoReq): PageDto<TodoDto> {
    val page = todoDao.findAll(req.toSpringDataPageRequest())
    val items = todoDtoAssembler.buildAll(page.content).toList()
    return PageDto(page.number + 1, page.totalPages, items, page.totalElements)
  }

  override fun getTodo(todoId: Int): TodoDto =
      todoDtoAssembler.build(todoDao.findById(todoId).orElseThrow { BusinessException(ApiErrorCode.NOT_FOUND) })

  override fun createTodo(req: CreateTodoReq): TodoDto {
    val entity = Todo().applyFrom(req)
    val saved = todoDao.saveAndFlush(entity)
    return todoDtoAssembler.build(saved)
  }

  override fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto {
    val entity = todoDao.findById(todoId).orElseThrow { BusinessException(ApiErrorCode.NOT_FOUND) }.applyFrom(req)
    val saved = todoDao.saveAndFlush(entity)
    return todoDtoAssembler.build(saved)
  }

  override fun deleteTodo(todoId: Int) {
    val entity = todoDao.findById(todoId).orElseThrow { BusinessException(ApiErrorCode.NOT_FOUND) }
    todoDao.delete(entity)
  }
}
