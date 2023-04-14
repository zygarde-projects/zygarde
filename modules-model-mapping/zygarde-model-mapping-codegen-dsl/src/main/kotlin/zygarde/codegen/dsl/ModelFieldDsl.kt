package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

abstract class ModelFieldDsl<E : Any>(
  val modelClass: KClass<E>,
  val dto: CodegenDto,
) {

  val dtoFieldMappings: MutableList<DtoFieldMapping> = mutableListOf()

  fun field(vararg fields: ModelMetaField, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    fields.forEach { f ->
      dtoFieldMappings.add(
        DtoFieldMapping.DtoFieldNoMapping(f, dto)
          .also(dsl)
      )
    }
  }

  fun fieldNullable(vararg fields: ModelMetaField, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    field(*fields) {
      dsl(this)
      forceNull = ForceNull.NULL
    }
  }

  fun fieldCollection(vararg fields: ModelMetaField, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    fields.forEach { f ->
      dtoFieldMappings.add(
        DtoFieldMapping.DtoFieldNoMapping(f, dto)
          .also(dsl)
          .also {
            it.refCollection = true
          }
      )
    }
  }

  fun fieldCollectionNullable(vararg fields: ModelMetaField, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    fieldCollection(*fields) {
      dsl(this)
      forceNull = ForceNull.NULL
    }
  }
}
