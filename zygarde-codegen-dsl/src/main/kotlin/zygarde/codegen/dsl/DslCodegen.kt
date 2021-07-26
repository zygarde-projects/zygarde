package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

abstract class DslCodegen<E : Any>(val modelClass: KClass<E>) {

  val dtoFieldMappings = mutableListOf<DtoFieldMapping>()

  fun execte() {
    codegen()
  }

  protected abstract fun codegen()

  /**
   * Mapping field to Dto, and also generate extension function 'toXXXDto' for Model
   */
  protected fun ModelMetaField<E, *>.toDto(
    vararg dtos: CodegenDto,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit)? = null
  ) {
    dtos.forEach { dto ->
      DtoFieldMapping.ModelToDtoFieldMappingVo(this, dto)
        .also {
          dsl?.invoke(it)
        }
        .also(dtoFieldMappings::add)
    }
  }

  protected fun ModelMetaField<E, *>.fieldFor(
    vararg dtos: CodegenDto,
    dsl: DtoFieldMapping.DtoFieldNoMapping.() -> Unit = { }
  ) {
    val modelFields = this
    dtos.forEach {
      it.fieldFrom(modelFields) { dsl.invoke(this) }
    }
  }

  protected fun ModelMetaField<E, *>.applyFrom(
    vararg dtos: CodegenDto,
    dsl: DtoFieldMapping.ModelApplyFromDtoFieldMappingVo.() -> Unit = { }
  ) {
    dtos.forEach { dto ->
      DtoFieldMapping.ModelApplyFromDtoFieldMappingVo(this, dto)
        .also(dsl)
        .also(dtoFieldMappings::add)
    }
  }

  protected inline fun <reified F : Any> extraField(
    fieldName: String,
    nullable: Boolean = false,
    dsl: (ModelMetaField<E, F>.() -> Unit) = {},
  ): ModelMetaField<E, F> {
    return ModelMetaField(modelClass, fieldName, F::class, nullable, true)
      .also(dsl)
  }

  protected inline fun <reified F : Any> extraCollectionField(
    fieldName: String,
    nullable: Boolean = false,
    dsl: (ModelMetaField<E, Collection<*>>.() -> Unit) = {},
  ): ModelMetaField<E, Collection<*>> {
    return ModelMetaField(modelClass, fieldName, Collection::class, nullable, true, arrayOf(F::class))
      .also(dsl)
  }

  /**
   * add field from model to Dto
   */
  protected fun CodegenDto.fieldFrom(
    vararg modelFields: ModelMetaField<E, *>,
    dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}
  ): CodegenDto {
    modelFields.forEach { modelField ->
      dtoFieldMappings.add(
        DtoFieldMapping.DtoFieldNoMapping(modelField, this)
          .also(dsl)
      )
    }
    return this
  }
}
