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
import zygarde.codegen.model.extensions.TodoDtoBuilder

@Service
public class TodoApiServiceImpl(
  @Autowired
  private val todoDao: TodoDao,
) : TodoApiService {
  override fun getTodoList(): Collection<TodoDto> = todoDao.findAll().map(TodoDtoBuilder::build)

  override fun getTodo(todoId: Int): TodoDto = todoDao.getById(todoId).let(TodoDtoBuilder::build)

  override fun createTodo(req: CreateTodoReq): TodoDto =
      Todo().applyFrom(req).let(todoDao::saveAndFlush).let(TodoDtoBuilder::build)

  override fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto =
      todoDao.getById(todoId).applyFrom(req).let(todoDao::saveAndFlush).let(TodoDtoBuilder::build)

  override fun deleteTodo(todoId: Int) {
    todoDao.deleteById(todoId)
  }
}
