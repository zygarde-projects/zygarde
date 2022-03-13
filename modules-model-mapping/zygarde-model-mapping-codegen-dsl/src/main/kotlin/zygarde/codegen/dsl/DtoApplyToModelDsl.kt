package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoFieldMapping.ModelApplyFromDtoFieldMappingVo
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

class DtoApplyToModelDsl<E : Any>(
  val modelClass: KClass<E>,
  val dtoFieldMappings: MutableList<DtoFieldMapping>,
  val dto: CodegenDto,
) {

  /**
   * generate a field in Dto and also generate extension function for Model.applyFrom for this Dto
   */
  fun applyTo(vararg fields: ModelMetaField<E, *>, dsl: (ModelApplyFromDtoFieldMappingVo.() -> Unit) = {}) {
    fields.forEach { f ->
      ModelApplyFromDtoFieldMappingVo(f, dto)
        .also(dsl)
        .also(dtoFieldMappings::add)
    }
  }

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

  fun ref(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldMapping.DtoFieldNoMapping(
        modelField = ModelMetaField(modelClass, fieldName, Any::class, nullable, extra = true),
        dto = dto
      )
        .also {
          it.dtoRef = dtoRef
        }
        .also(dsl)
    )
  }

  fun refCollection(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldMapping.DtoFieldNoMapping(
        modelField = ModelMetaField(modelClass, fieldName, Any::class, nullable, extra = true),
        dto = dto
      )
        .also {
          it.dtoRef = dtoRef
          it.refCollection = true
        }
        .also(dsl)
    )
  }
}
