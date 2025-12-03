package zygarde.samples.todo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.AdditionalDtoProp
import zygarde.codegen.AdditionalDtoProps
import zygarde.codegen.ApiPathVariable
import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.GenApi
import zygarde.codegen.RequestDto
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ZyApi
import zygarde.codegen.ZyModel
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.data.jpa.dao.search
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.option.OptionEnum
import zygarde.samples.todo.generated.dao.TodoDao
import zygarde.samples.todo.generated.dto.CreateTodoReq
import zygarde.samples.todo.generated.dto.TodoDto
import zygarde.samples.todo.generated.dto.UpdateTodoReq
import zygarde.samples.todo.generated.dto.applyFromCreateTodoReq
import zygarde.samples.todo.generated.dto.applyFromUpdateTodoReq
import zygarde.samples.todo.generated.dto.toTodoDto
import zygarde.samples.todo.generated.search.description
import zygarde.samples.todo.generated.service.TodoService
import javax.persistence.Entity

const val createTodoReq = "CreateTodoReq"
const val updateTodoReq = "UpdateTodoReq"
const val todoDto = "TodoDto"

/**
 * Sample entity demonstrating KSP code generation.
 * The @ZyModel annotation triggers:
 * - Dao interface generation (TodoDao)
 * - Search extension function generation (description())
 * - DTO generation from @ApiProp annotations
 */
@ZyModel
@Entity
@AdditionalDtoProps(
  [
    AdditionalDtoProp(
      forDto = [todoDto],
      field = "id",
      fieldType = Int::class,
      comment = "id of Todo",
      entityValueProvider = AutoIntIdValueProvider::class
    )
  ]
)
class Todo(
  @ApiProp(
    comment = "description of todo",
    dto = [Dto(todoDto)],
    requestDto = [
      RequestDto(createTodoReq, forceNullableInReq = true),
      RequestDto(updateTodoReq),
    ]
  )
  var description: String = "",
  @ApiProp(
    comment = "priority of todo",
    dto = [Dto(todoDto)],
    requestDto = [
      RequestDto(createTodoReq),
      RequestDto(updateTodoReq),
    ]
  )
  var priority: Int = 0
) : AutoIntIdEntity()

/**
 * Sample service implementation using generated code.
 */
@Service
class MyTodoService(
  @Autowired val todoDao: TodoDao
) : TodoService {
  fun searchByDescription(description: String): List<Todo> {
    return todoDao.search {
      description() eq description
    }
  }

  override fun createTodo(req: CreateTodoReq): TodoDto {
    return Todo().applyFromCreateTodoReq(req).let(todoDao::save).toTodoDto()
  }

  override fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto {
    return todoDao.getById(todoId).applyFromUpdateTodoReq(req).let(todoDao::save).toTodoDto()
  }

  override fun listTodo(): Collection<TodoDto> {
    return todoDao.findAll().map { it.toTodoDto() }
  }
}

/**
 * API specification for Todo endpoints.
 */
@ZyApi(
  group = "todo",
  api = [
    GenApi(
      method = RequestMethod.GET,
      path = "/api/todo",
      api = "TodoApi.listTodo",
      apiDescription = "List todo",
      service = "TodoService.listTodo",
      resRef = todoDto,
      resCollection = true,
    ),
    GenApi(
      method = RequestMethod.POST,
      path = "/api/todo",
      api = "TodoApi.createTodo",
      apiDescription = "Create todo",
      service = "TodoService.createTodo",
      reqRef = createTodoReq,
      resRef = todoDto,
    ),
    GenApi(
      method = RequestMethod.PUT,
      path = "/api/todo/{todoId}",
      pathVariable = [ApiPathVariable("todoId", Int::class)],
      api = "TodoApi.updateTodo",
      apiDescription = "Update todo",
      service = "TodoService.updateTodo",
      reqRef = updateTodoReq,
      resRef = todoDto,
    ),
  ]
)
object TodoApiSpec

/**
 * Sample enum for static option API generation.
 */
@StaticOptionApi(comment = "Todo Status")
enum class TodoStatus(
  override val label: String
) : OptionEnum {
  DOING("doing"),
  DONE("done"),
}
