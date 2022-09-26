package example.codegen.model.meta

import example.Note
import kotlin.Int
import kotlin.String
import zygarde.codegen.meta.ModelMetaField

public object NoteModelFields {
  public val title: ModelMetaField<Note, String> = ModelMetaField(
    modelClass = Note::class,
    fieldName = "title",
    fieldClass = String::class,
    fieldNullable = false,
    comment = "",
    genericClasses = arrayOf()
  )

  public val id: ModelMetaField<Note, Int> = ModelMetaField(
    modelClass = Note::class,
    fieldName = "id",
    fieldClass = Int::class,
    fieldNullable = false,
    comment = "",
    genericClasses = arrayOf()
  )
}
