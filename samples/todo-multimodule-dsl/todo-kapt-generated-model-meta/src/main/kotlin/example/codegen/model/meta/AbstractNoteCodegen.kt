package example.codegen.model.meta

import example.Note
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Unit
import zygarde.codegen.dsl.ClassBasedModelMappingDslCodegen
import zygarde.codegen.meta.ModelMetaField

public abstract class AbstractNoteCodegen : ClassBasedModelMappingDslCodegen<Note>(Note::class) {
  protected val title: ModelMetaField<Note, String> = NoteModelFields.title

  protected val id: ModelMetaField<Note, Int> = NoteModelFields.id

  public val allFields: Array<ModelMetaField<Note, *>> = arrayOf(title, id)

  public fun title(dsl: ModelMetaField<Note, String>.() -> Unit) {
    dsl.invoke(title)
  }

  public fun id(dsl: ModelMetaField<Note, Int>.() -> Unit) {
    dsl.invoke(id)
  }
}
