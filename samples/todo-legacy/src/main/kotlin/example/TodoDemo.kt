package example

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
import zygarde.codegen.ZyApi
import zygarde.codegen.ZyModel
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.data.jpa.dao.search
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.generated.data.dao.TodoDao
import zygarde.generated.data.dto.CreateToDoReq
import zygarde.generated.data.dto.TodoDto
import zygarde.generated.data.dto.UpdateToDoReq
import zygarde.generated.data.dto.applyFromCreateToDoReq
import zygarde.generated.data.dto.applyFromUpdateToDoReq
import zygarde.generated.data.dto.toTodoDto
import zygarde.generated.entity.search.description
import zygarde.generated.service.TodoService
import javax.persistence.Entity

const val createToDoReq = "CreateToDoReq"
const val updateToDoReq = "UpdateToDoReq"
const val todoDto = "TodoDto"

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
      RequestDto(createToDoReq),
      RequestDto(updateToDoReq),
    ]
  )
  var description: String = "",
) : AutoIntIdEntity()

@Service
class MyTodoService(@Autowired val todoDao: TodoDao) : TodoService {

  fun searchTodoByDescription(description: String): Collection<Todo> {
    return todoDao.search {
      description() eq description
    }
  }

  override fun createTodo(req: CreateToDoReq): TodoDto {
    return Todo().applyFromCreateToDoReq(req).let(todoDao::save).toTodoDto()
  }

  override fun updateTodo(todoId: Int, req: UpdateToDoReq): TodoDto {
    return todoDao.getById(todoId).applyFromUpdateToDoReq(req).let(todoDao::save).toTodoDto()
  }

  override fun listTodo(): Collection<TodoDto> {
    return todoDao.findAll().map { it.toTodoDto() }
  }
}

@ZyApi(
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
      reqRef = createToDoReq,
      resRef = todoDto,
    ),
    GenApi(
      method = RequestMethod.PUT,
      path = "/api/todo/{todoId}",
      pathVariable = [ApiPathVariable("todoId", Int::class)],
      api = "TodoApi.updateTodo",
      apiDescription = "Update todo",
      service = "TodoService.updateTodo",
      reqRef = updateToDoReq,
      resRef = todoDto,
    ),
  ]
)
object TodoApiSpec
