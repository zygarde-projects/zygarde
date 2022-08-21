package example.codegen.model.meta

import example.Todo
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Unit
import zygarde.codegen.dsl.ClassBasedModelMappingDslCodegen
import zygarde.codegen.meta.ModelMetaField

public abstract class AbstractTodoCodegen : ClassBasedModelMappingDslCodegen<Todo>(Todo::class) {
  protected val description: ModelMetaField<Todo, String> = TodoModelFields.description

  protected val id: ModelMetaField<Todo, Int> = TodoModelFields.id

  public val allFields: Array<ModelMetaField<Todo, *>> = arrayOf(description,id)

  public fun description(dsl: ModelMetaField<Todo, String>.() -> Unit): Unit {
    dsl.invoke(description)
  }

  public fun id(dsl: ModelMetaField<Todo, Int>.() -> Unit): Unit {
    dsl.invoke(id)
  }
}
