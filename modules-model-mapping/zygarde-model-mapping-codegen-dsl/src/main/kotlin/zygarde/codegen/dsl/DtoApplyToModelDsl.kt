package zygarde.codegen.dsl

import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoFieldMapping.ModelApplyFromDtoFieldMappingVo
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

class DtoApplyToModelDsl<E : Any>(
  modelClass: KClass<E>,
  dto: CodegenDto,
) : ModelFieldDsl<E>(
  modelClass,
  dto,
) {

  /**
   * generate a field in Dto and also generate extension function for Model.applyFrom for this Dto
   */
  fun applyTo(vararg fields: ModelMetaField, dsl: (ModelApplyFromDtoFieldMappingVo.() -> Unit) = {}) {
    fields.forEach { f ->
      ModelApplyFromDtoFieldMappingVo(f, dto)
        .also(dsl)
        .also(dtoFieldMappings::add)
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
        modelField = ModelMetaField(modelClass.asTypeName(), fieldName, Any::class.asTypeName(), nullable, extra = true),
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
        modelField = ModelMetaField(modelClass.asTypeName(), fieldName, Any::class.asTypeName(), nullable, extra = true),
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
