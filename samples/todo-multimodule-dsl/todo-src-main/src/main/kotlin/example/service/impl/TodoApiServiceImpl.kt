package example.service.impl

import example.Todo
import example.codegen.data.dao.TodoDao
import example.service.TodoApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.TodoDto
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.codegen.model.extensions.TodoApplyValueExtensions.applyFrom
import zygarde.codegen.model.extensions.TodoToDtoExtensions.toTodoDto
import java.util.function.Consumer

@Service
class TodoApiServiceImpl(
  @Autowired val todoDao: TodoDao,
) : TodoApiService {
  override fun getTodoList(): Collection<TodoDto> {
    return todoDao.findAll().map { it.toTodoDto() }
  }

  override fun getTodo(todoId: Int): TodoDto {
    return todoDao.getById(todoId).toTodoDto()
  }

  override fun createTodo(req: CreateTodoReq, postProcessingParamConsumer: Consumer<String>): TodoDto {
    return Todo().applyFrom(req).let(todoDao::saveAndFlush).toTodoDto().also { postProcessingParamConsumer.accept("created") }
  }

  override fun createTodoPostProcessing(req: CreateTodoReq, result: TodoDto, extraParam: String) {
    println(extraParam)
  }

  override fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto {
    return todoDao.getById(todoId).applyFrom(req).let(todoDao::saveAndFlush).toTodoDto()
  }

  override fun deleteTodo(todoId: Int) {
    todoDao.deleteById(todoId)
  }
}
