package zygarde.codegen.meta

import kotlin.reflect.KClass

data class EntityMetaField<E : Any, F : Any>(
  val entityClass: KClass<E>,
  val fieldName: String,
  val fieldClass: KClass<F>,
  val fieldNullable: Boolean,
)
