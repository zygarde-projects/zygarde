package example.codegen.model.meta

import example.AbstractNote
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Unit
import zygarde.codegen.dsl.ClassBasedModelMappingDslCodegen
import zygarde.codegen.meta.ModelMetaField

public abstract class AbstractAbstractNoteCodegen :
    ClassBasedModelMappingDslCodegen<AbstractNote>(AbstractNote::class) {
  protected val title: ModelMetaField<AbstractNote, String> = AbstractNoteModelFields.title

  protected val id: ModelMetaField<AbstractNote, Int> = AbstractNoteModelFields.id

  public val allFields: Array<ModelMetaField<AbstractNote, *>> = arrayOf(title,id)

  public fun title(dsl: ModelMetaField<AbstractNote, String>.() -> Unit): Unit {
    dsl.invoke(title)
  }

  public fun id(dsl: ModelMetaField<AbstractNote, Int>.() -> Unit): Unit {
    dsl.invoke(id)
  }
}
