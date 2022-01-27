
## Generate Dao and field hints for search 

* Dependencies
```
dependencies {
  implementation(project(":zygarde-jpa"))
  kapt(project(":zygarde-jpa-codegen"))
}
```

* First, we create an entity and annotated with `@ZyModel`

```
@ZyModel
@Entity
class Todo(
  var description: String = "",
) : AutoIntIdEntity()
```

* run `./gradlew kaptKotlin`

code will generate to build/generated/source/kaptKotlin/main/zygarde/generated

* then, we can inject the generated `TodoDao` and start to use it.

```
@Service
class MyTodoService(@Autowired val todoDao: TodoDao) {

  fun searchTodoByDescription(description: String): Collection<Todo> {
    return todoDao.search {
      description() eq description
    }
  }
}
```

## Generate Dto mapping for Todo

* Dependencies
```
dependencies {
  implementation(project(":zygarde-model-mapping"))
  kapt(project(":zygarde-model-mapping-codegen"))
}
```

* add more annotations to Todo to declare the field relationship for Dto

```
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
```

* run `./gradlew kaptKotlin`

the dto classes `CreateToDoReq`, `UpdateToDoReq` and `TodoDto` will be generated
and the extension function `Todo.toTodoDto`, `Todo.applyFromCreateToDoReq`, `Todo.applyFromUpdateToDoReq` will also generated. 

* then we can use the generated extension functions to easily control the mapping between entity and DTO.

```
fun createTodo(req: CreateToDoReq): TodoDto {
  return Todo().applyFromCreateToDoReq(req).let(todoDao::save).toTodoDto()
}
fun updateTodo(id: Int, req: UpdateToDoReq): TodoDto {
  return todoDao.getOne(id).applyFromUpdateToDoReq(req).let(todoDao::save).toTodoDto()
}
fun listTodo(): Collection<TodoDto> {
  return todoDao.findAll().map { it.toTodoDto() }
}
```

## Generate Controller and Service Interface for Todo

* Dependencies
```
dependencies {
  implementation(project(":zygarde-webmvc"))
  kapt(project(":zygarde-webmvc-codegen"))
}
```

* Write api spec defining api with annotations 
```
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
```

* run `./gradlew kaptKotlin`

the interface and implementation `TodoApi`, `TodoApiImpl`(Controller) and `TodoService` will be generated

* then, modify MyTodoService to implement TodoService, the api implementation is done

```

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
    return todoDao.getOne(todoId).applyFromUpdateToDoReq(req).let(todoDao::save).toTodoDto()
  }

  override fun listTodo(): Collection<TodoDto> {
    return todoDao.findAll().map { it.toTodoDto() }
  }
}
```
