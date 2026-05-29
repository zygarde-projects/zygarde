package example.service.`impl`

import example.Todo
import example.codegen.`data`.dao.TodoDao
import example.service.TodoApiService
import kotlin.Int
import kotlin.collections.Collection
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq
import zygarde.codegen.model.extensions.TodoApplyValueExtensions.applyFrom
import zygarde.codegen.model.extensions.TodoDtoAssembler

@Service
public class TodoApiServiceImpl(
  @Autowired
  private val todoDao: TodoDao,
  @Autowired
  private val todoDtoAssembler: TodoDtoAssembler,
) : TodoApiService {
  override fun getTodoList(): Collection<TodoDto> = todoDtoAssembler.buildAll(todoDao.findAll())

  override fun getTodo(todoId: Int): TodoDto = todoDtoAssembler.build(todoDao.getById(todoId))

  override fun createTodo(req: CreateTodoReq): TodoDto =
      todoDtoAssembler.build(Todo().applyFrom(req).let(todoDao::saveAndFlush))

  override fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto =
      todoDtoAssembler.build(todoDao.getById(todoId).applyFrom(req).let(todoDao::saveAndFlush))

  override fun deleteTodo(todoId: Int) {
    todoDao.deleteById(todoId)
  }
}
