package example.graphql

import example.FileDto
import example.FileDtoProvider
import example.graphql.service.TodoGraphQlService
import example.graphql.service.TodoGraphQlSource
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.Map
import org.springframework.graphql.`data`.method.`annotation`.Argument
import org.springframework.graphql.`data`.method.`annotation`.BatchMapping
import org.springframework.graphql.`data`.method.`annotation`.MutationMapping
import org.springframework.graphql.`data`.method.`annotation`.QueryMapping
import org.springframework.stereotype.Controller
import zygarde.`data`.provider.DataProviderContext
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.UpdateTodoReq
import zygarde.core.di.DiServiceContext.bean

@Controller
public class TodoGraphQlController {
  @QueryMapping(name = "todos")
  public fun todos(@Argument(name = "filter") filter: TodoFilter?): Collection<TodoGraphQlSource> {
    val service = bean<TodoGraphQlService>()
    return service.todos(filter)
  }

  @QueryMapping(name = "todo")
  public fun todo(@Argument(name = "id") id: Int): TodoGraphQlSource? {
    val service = bean<TodoGraphQlService>()
    return service.todo(id)
  }

  @QueryMapping(name = "todosByIds")
  public fun todosByIds(@Argument(name = "ids") ids: Collection<Int>):
      Collection<TodoGraphQlSource> {
    val service = bean<TodoGraphQlService>()
    return service.todosByIds(ids)
  }

  @MutationMapping(name = "createTodo")
  public fun createTodo(@Argument(name = "input") input: CreateTodoReq): TodoGraphQlSource {
    val service = bean<TodoGraphQlService>()
    return service.createTodo(input)
  }

  @MutationMapping(name = "updateTodo")
  public fun updateTodo(@Argument(name = "id") id: Int, @Argument(name = "input")
      input: UpdateTodoReq): TodoGraphQlSource {
    val service = bean<TodoGraphQlService>()
    return service.updateTodo(id, input)
  }

  @MutationMapping(name = "deleteTodo")
  public fun deleteTodo(@Argument(name = "id") id: Int): Boolean {
    val service = bean<TodoGraphQlService>()
    return service.deleteTodo(id)
  }

  @BatchMapping(
    typeName = "Todo",
    field = "file",
  )
  public fun todoFile(items: List<TodoGraphQlSource>): Map<TodoGraphQlSource, FileDto> {
    val fileDtoProvider = bean<FileDtoProvider>()
    val keys = items.mapNotNull { it.fileId }.distinct()
    val values = fileDtoProvider.load(keys, DataProviderContext.EMPTY)
    return items.mapNotNull { item ->
        val value = item.fileId?.let(values::get)
        value?.let { item to it }
        }.toMap()
  }
}
