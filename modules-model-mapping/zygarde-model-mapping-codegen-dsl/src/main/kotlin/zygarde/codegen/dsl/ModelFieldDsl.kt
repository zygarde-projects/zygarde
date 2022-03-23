package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

abstract class ModelFieldDsl<E : Any>(
  val modelClass: KClass<E>,
  val dtoFieldMappings: MutableList<DtoFieldMapping>,
  val dto: CodegenDto,
) {

  fun field(vararg fields: ModelMetaField<E, *>, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    fields.forEach { f ->
      dtoFieldMappings.add(
        DtoFieldMapping.DtoFieldNoMapping(f, dto)
          .also(dsl)
      )
    }
  }

  fun fieldCollection(vararg fields: ModelMetaField<E, *>, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
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
}
