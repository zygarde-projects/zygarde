package example.codegen.model.meta

import example.Todo
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.Collection
import zygarde.codegen.dsl.ModelMappingDslCodegen
import zygarde.codegen.meta.ModelMetaField

public abstract class AbstractTodoCodegen : ModelMappingDslCodegen<Todo>(Todo::class) {
  protected val id: ModelMetaField<Todo, Int> = ModelMetaField(
        modelClass=Todo::class,
        fieldName="id",
        fieldClass=Int::class,
        fieldNullable=false,
        comment="",
        genericClasses=arrayOf()
      )


  protected val description: ModelMetaField<Todo, String> = ModelMetaField(
        modelClass=Todo::class,
        fieldName="description",
        fieldClass=String::class,
        fieldNullable=false,
        comment="",
        genericClasses=arrayOf()
      )


  public val allFields: Collection<ModelMetaField<Todo, *>> = listOf(id,description)

  public fun id(dsl: ModelMetaField<Todo, Int>.() -> Unit): Unit {
    dsl.invoke(id)
  }

  public fun description(dsl: ModelMetaField<Todo, String>.() -> Unit): Unit {
    dsl.invoke(description)
  }

  public fun mapAllFields(dsl: ModelMetaField<Todo, *>.() -> Unit): Unit {
    allFields.forEach{ dsl.invoke(it) }
  }
}
