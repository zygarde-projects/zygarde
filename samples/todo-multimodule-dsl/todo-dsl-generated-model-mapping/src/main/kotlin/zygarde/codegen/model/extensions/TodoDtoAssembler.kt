package zygarde.codegen.model.extensions

import example.FileDtoProvider
import example.Todo
import kotlin.collections.Collection
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Component
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`value`.AutoIntIdValueProvider

@Component
public class TodoDtoAssembler(
  @Autowired
  private val fileDtoProvider: FileDtoProvider,
) {
  public fun build(model: Todo): TodoDto = buildAll(listOf(model)).single()

  public fun buildAll(models: Collection<Todo>): Collection<TodoDto> {
    val modelList = models.toList()
    val fileKeys = modelList.mapNotNull { it.fileId }.distinct()
    val fileValues = fileDtoProvider.load(fileKeys)
    return modelList.map { model ->
        TodoDto(
          id = AutoIntIdValueProvider().getValue(model),
          description = model.description,
          file = model.fileId?.let { fileValues[it] }
        )
        }
  }
}
