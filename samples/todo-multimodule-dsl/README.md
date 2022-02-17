
# Model Mapping

## Configure model-meta for model-mapping dsl codegen

* Create module `todo-src-core` and create Entity `Todo` 

```
@Entity
@ZyModelMeta
class Todo(
  var description: String = "",
) : AutoIntIdEntity()
```

* Create module `todo-kapt-generated-model-meta` 
* Config `todo-src-core/build.gradle.kts` to define model-meta codegen target
```
kapt {
  arguments {
    arg("zygarde.codegen.meta.target.folder", project(":todo-kapt-generated-model-meta").file("src/main/kotlin").absolutePath)
  }
}
```

* Then run `./gradlew :todo-src-core:kaptkotlin`
* The dsl codegen base class `AbstractTodoCodegen` will be generated to `todo-kapt-generated-model-meta/src/main/kotlin`

## Write DSL to define model relationship for `Todo`

* Create module `todo-codegen-dsl-models` and write `TodoModelDslCodegen`

```
class TodoModelDslCodegen : AbstractTodoCodegen() {

  enum class TodoDtos : CodegenDtoSimple {
    TodoDto,
    CreateTodoReq,
    UpdateTodoReq,
  }

  override fun codegen() {
    id {
      toDto(TodoDtos.TodoDto) {
        valueProvider = AutoIntIdValueProvider::class
        valueProviderParameterType = ValueProviderParameterType.OBJECT
      }
    }

    description {
      toDto(TodoDtos.TodoDto)
      applyFrom(TodoDtos.CreateTodoReq, TodoDtos.UpdateTodoReq)
    }
  }
}

```

* Create module `todo-dsl-generated-model-mapping`
* Configure `todo-codegen-dsl-models/build.gradle.kts` to define codegen target folder for model-mapping

```
configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.DslMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.target=${project(":todo-dsl-generated-model-mapping").file("src/main/kotlin").absolutePath}"
  )
}
```

* Run `./gradlew :todo-codegen-dsl-models:run`

* The `TodoDto`, `CreateTodoReq`, `UpdateTodoReq` and extensions for Todo will be generated in `todo-dsl-generated-model-mapping/src/main/kotlin`


# API, Controller and Service Interface Codegen

* Create module `todo-codegen-dsl-apis` and write `TodoApiCodegen`

```

class TodoApiCodegen : WebMvcDslCodegen() {
  override fun codegen() {

    api("TodoApi", "/api/todo") {
      fun DslApiFunction.todoIdPathVariable() {
        pathVariable<Int>("todoId")
      }

      get("getTodoList", "") {
        resCollection<TodoDto>()
      }
      get("getTodo", "{todoId}") {
        todoIdPathVariable()
        res<TodoDto>()
      }
      post("createTodo", "") {
        req<CreateTodoReq>()
        res<TodoDto>()
      }
      put("updateTodo", "{todoId}") {
        todoIdPathVariable()
        req<UpdateTodoReq>()
        res<TodoDto>()
      }
      delete("deleteTodo", "{todoId}") {
        todoIdPathVariable()
      }
    }
  }
}

```
* create modules for api codegen target
    * `todo-dsl-generated-api-interface`
    * `todo-dsl-generated-controller`
    * `todo-dsl-generated-feign`
    * `todo-dsl-generated-service-interface`
    
* Configure `todo-codegen-dsl-apis/build.gradle.kts` to map codegen target

```

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.webmvc.WebMvcDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.webmvc.api-interface.package=example.api",
    "-Dzygarde.codegen.dsl.webmvc.controller.package=example.controller",
    "-Dzygarde.codegen.dsl.webmvc.service-interface.package=example.service",
    "-Dzygarde.codegen.dsl.webmvc.api-interface.write-to=${project(":todo-dsl-generated-api-interface").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.webmvc.feign-interface.write-to=${project(":todo-dsl-generated-feign").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.webmvc.controller.write-to=${project(":todo-dsl-generated-controller").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.webmvc.service-interface.write-to=${project(":todo-dsl-generated-service-interface").file("src/main/kotlin").absolutePath}"
  )
}

```

* Run `./gradlew :todo-codegen-dsl-apis:run` 
  * `TodoApi` will be generated to `todo-dsl-generated-api-interface/src/main/kotlin`
  * `TodoApiController` will be generated to `todo-dsl-generated-controller/src/main/kotlin`
  * `TodoApiFeign` will be generated to `todo-dsl-generated-feign/src/main/kotlin`
  * `TodoApiService` will be generated to `todo-dsl-generated-service-interface/src/main/kotlin`
  
  
# Implement Business logic

* Create module `todo-src-main`

* Implement `TodoApiServiceImpl`

```
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

  override fun createTodo(req: CreateTodoReq): TodoDto {
    return Todo().applyFrom(req).let(todoDao::saveAndFlush).toTodoDto()
  }

  override fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto {
    return todoDao.getById(todoId).applyFrom(req).let(todoDao::saveAndFlush).toTodoDto()
  }

  override fun deleteTodo(todoId: Int) {
    todoDao.deleteById(todoId)
  }
}
```
