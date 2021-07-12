package zygarde.codegen.meta

import kotlin.reflect.KClass

data class ModelMetaField<E : Any, F : Any>(
  val modelClass: KClass<E>,
  val fieldName: String,
  val fieldClass: KClass<F>,
  val fieldNullable: Boolean,
  val extra: Boolean = false,
  val genericClasses: Array<KClass<*>> = emptyArray(),
  var comment: String = "",
)
