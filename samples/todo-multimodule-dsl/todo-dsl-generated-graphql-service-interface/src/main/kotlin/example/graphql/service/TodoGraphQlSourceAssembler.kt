package example.graphql.service

import example.Todo
import kotlin.collections.Collection
import zygarde.codegen.`value`.AutoIntIdValueProvider

public class TodoGraphQlSourceAssembler {
  public fun build(model: Todo): TodoGraphQlSource = buildAll(listOf(model)).single()

  public fun buildAll(models: Collection<Todo>): Collection<TodoGraphQlSource> = models.map {
      model ->
  TodoGraphQlSource(
    id = AutoIntIdValueProvider().getValue(model),
    description = model.description,
    fileId = model.fileId
  )
  }
}
