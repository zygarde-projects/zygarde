package example.codegen.model.meta

import example.Todo
import kotlin.Int
import kotlin.String
import zygarde.codegen.meta.ModelMetaField

public object TodoModelFields {
  public val description: ModelMetaField<Todo, String> = ModelMetaField(
    modelClass = Todo::class,
    fieldName = "description",
    fieldClass = String::class,
    fieldNullable = false,
    comment = "",
    genericClasses = arrayOf()
  )

  public val id: ModelMetaField<Todo, Int> = ModelMetaField(
    modelClass = Todo::class,
    fieldName = "id",
    fieldClass = Int::class,
    fieldNullable = false,
    comment = "",
    genericClasses = arrayOf()
  )
}
