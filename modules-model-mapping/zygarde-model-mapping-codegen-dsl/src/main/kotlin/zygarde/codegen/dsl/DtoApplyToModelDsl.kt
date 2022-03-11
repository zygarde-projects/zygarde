package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoFieldMapping.DtoFieldNoMapping
import zygarde.codegen.dsl.model.internal.DtoFieldMapping.ModelApplyFromDtoFieldMappingVo
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

class DtoApplyToModelDsl<E : Any>(
  private val modelClass: KClass<E>,
  private val dtoFieldMappings: MutableList<DtoFieldMapping>,
  private val dto: CodegenDto,
) {

  fun toModel(vararg fields: ModelMetaField<E, *>, dsl: (ModelApplyFromDtoFieldMappingVo.() -> Unit) = {}) {
    fields.forEach { f ->
      ModelApplyFromDtoFieldMappingVo(f, dto)
        .also(dsl)
        .also(dtoFieldMappings::add)
    }
  }

  fun extra(vararg fields: ModelMetaField<E, *>, dsl: (DtoFieldNoMapping.() -> Unit) = {}) {
    fields.forEach { f ->
      dtoFieldMappings.add(
        DtoFieldNoMapping(f, dto)
          .also(dsl)
      )
    }
  }

  fun extra(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldNoMapping.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldNoMapping(
        modelField = ModelMetaField(modelClass, fieldName, Any::class, nullable, extra = true),
        dto = dto
      )
        .also {
          it.dtoRef = dtoRef
        }
        .also(dsl)
    )
  }

  fun extraCollection(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldNoMapping.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldNoMapping(
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
