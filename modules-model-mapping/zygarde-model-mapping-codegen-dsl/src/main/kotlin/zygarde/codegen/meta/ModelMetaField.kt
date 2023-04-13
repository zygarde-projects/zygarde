package zygarde.codegen.meta

import java.lang.reflect.Type
import kotlin.reflect.KClass

data class ModelMetaField<E : Any, F : Any>(
  val modelClass: KClass<E>,
  val fieldName: String,
  val fieldClass: KClass<F>,
  val fieldNullable: Boolean,
  val extra: Boolean = false,
  val genericClasses: Array<Type> = emptyArray(),
  var comment: String = "",
)
