package example.codegen.model.meta

import example.AbstractNote
import kotlin.Int
import kotlin.String
import zygarde.codegen.meta.ModelMetaField

public object AbstractNoteModelFields {
  public val title: ModelMetaField<AbstractNote, String> = ModelMetaField(
        modelClass=AbstractNote::class,
        fieldName="title",
        fieldClass=String::class,
        fieldNullable=false,
        comment="",
        genericClasses=arrayOf()
      )


  public val id: ModelMetaField<AbstractNote, Int> = ModelMetaField(
        modelClass=AbstractNote::class,
        fieldName="id",
        fieldClass=Int::class,
        fieldNullable=false,
        comment="",
        genericClasses=arrayOf()
      )

}
