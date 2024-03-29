package zygarde.codegen.dsl

import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.dsl.model.internal.DtoFieldMapping.ModelToDtoFieldMappingVo
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.AutoLongIdValueProvider
import zygarde.codegen.value.ValueProvider
import kotlin.reflect.KClass

class ModelToDtoDsl<E : Any>(
  modelClass: KClass<E>,
  dto: CodegenDto,
) : ModelFieldDsl<E>(
  modelClass,
  dto,
) {

  /**
   * add a field to Dto and also generate extension function {Model}.toDto
   */
  fun from(vararg fields: ModelMetaField, dsl: (ModelToDtoFieldMappingVo.() -> Unit) = { }) {
    fields.forEach { f ->
      ModelToDtoFieldMappingVo(modelField = f, dto = dto)
        .also(dsl)
        .also(dtoFieldMappings::add)
    }
  }

  fun fieldExtra(vararg fields: ModelMetaField, dsl: (ModelToDtoFieldMappingVo.() -> Unit) = {}) {
    fields.forEach { f ->
      dtoFieldMappings.add(
        ModelToDtoFieldMappingVo(f.copy(extra = true), dto)
          .also(dsl)
      )
    }
  }

  fun fromAutoIntId(vararg fields: ModelMetaField) {
    fromObjectProvider<AutoIntIdValueProvider>(*fields)
  }

  fun fromAutoLongId(vararg fields: ModelMetaField) {
    fromObjectProvider<AutoLongIdValueProvider>(*fields)
  }

  fun ref(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (ModelToDtoFieldMappingVo.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      ModelToDtoFieldMappingVo(
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
    dsl: (ModelToDtoFieldMappingVo.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      ModelToDtoFieldMappingVo(
        modelField = ModelMetaField(modelClass.asTypeName(), fieldName, Any::class.asClassName(), nullable, extra = true),
        dto = dto
      )
        .also {
          it.dtoRef = dtoRef
          it.refCollection = true
        }
        .also(dsl)
    )
  }

  inline fun <reified P : ValueProvider<*, *>> fromObjectProvider(
    vararg fields: ModelMetaField,
  ) {
    from(*fields) {
      valueProvider = P::class.asClassName()
      valueProviderParameterType = ValueProviderParameterType.OBJECT
    }
  }

  inline fun <reified P : ValueProvider<*, *>> fromFieldProvider(
    vararg fields: ModelMetaField,
  ) {
    from(*fields) {
      valueProvider = P::class.asClassName()
      valueProviderParameterType = ValueProviderParameterType.OBJECT
    }
  }
}
