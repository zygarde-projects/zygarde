package zygarde.codegen.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class ModelMetaField(
  val modelClass: ClassName,
  val fieldName: String,
  val fieldClass: TypeName,
  val fieldNullable: Boolean,
  val extra: Boolean = false,
  var comment: String = "",
)
