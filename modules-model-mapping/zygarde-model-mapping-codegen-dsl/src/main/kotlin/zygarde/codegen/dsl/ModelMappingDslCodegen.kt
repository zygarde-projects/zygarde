package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.AutoLongIdValueProvider
import kotlin.reflect.KClass

abstract class ModelMappingDslCodegen<E : Any>(val modelClass: KClass<E>) {

  val dtoFieldMappings = mutableListOf<DtoFieldMapping>()

  fun execute() {
    codegen()
  }

  protected abstract fun codegen()

  protected fun ModelMetaField<E, Int>.toDtoWithAutoIntIdProvider(vararg dtos: CodegenDto) {
    this.toDto(*dtos) {
      valueProvider = AutoIntIdValueProvider::class
      valueProviderParameterType = ValueProviderParameterType.OBJECT
    }
  }

  protected fun ModelMetaField<E, Long>.toDtoWithAutoLongIdProvider(vararg dtos: CodegenDto) {
    this.toDto(*dtos) {
      valueProvider = AutoLongIdValueProvider::class
      valueProviderParameterType = ValueProviderParameterType.OBJECT
    }
  }

  /**
   * Mapping field to Dto, and also generate extension function 'toXXXDto' for Model
   */
  protected fun ModelMetaField<E, *>.toDto(
    vararg dtos: CodegenDto,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit)? = null
  ) {
    dtos.forEach { dto ->
      DtoFieldMapping.ModelToDtoFieldMappingVo(modelField = this, dto = dto)
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

  protected fun CodegenDto.fieldRefDto(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}
  ): CodegenDto {
    dtoFieldMappings.add(
      DtoFieldMapping.DtoFieldNoMapping(
        modelField = ModelMetaField(modelClass, fieldName, Any::class, nullable, extra = true),
        dto = this
      )
        .also {
          it.dtoRef = dtoRef
        }
        .also(dsl)
    )
    return this
  }

  protected fun CodegenDto.fieldRefToDto(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = {}
  ): CodegenDto {
    dtoFieldMappings.add(
      DtoFieldMapping.ModelToDtoFieldMappingVo(
        modelField = ModelMetaField(modelClass, fieldName, Any::class, nullable, extra = true),
        dto = this
      )
        .also {
          it.dtoRef = dtoRef
        }
        .also(dsl)
    )
    return this
  }

  protected fun CodegenDto.fieldRefToDtoCollection(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = {}
  ): CodegenDto {
    dtoFieldMappings.add(
      DtoFieldMapping.ModelToDtoFieldMappingVo(
        modelField = ModelMetaField(modelClass, fieldName, Any::class, nullable, extra = true),
        dto = this
      )
        .also {
          it.dtoRef = dtoRef
          it.refCollection = true
        }
        .also(dsl)
    )
    return this
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

  /**
   * alternative way of ModelMetaField<E, *>.toDto
   */
  protected fun CodegenDto.modelToDto(
    vararg modelFields: ModelMetaField<E, *>,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = {}
  ): CodegenDto {
    modelFields.forEach { it.toDto(this, dsl = dsl) }
    return this
  }

  /**
   * alternative way of ModelMetaField<E, *>.applyFrom
   */
  protected fun CodegenDto.dtoToModel(
    vararg modelFields: ModelMetaField<E, *>,
    dsl: (DtoFieldMapping.ModelApplyFromDtoFieldMappingVo.() -> Unit) = {}
  ): CodegenDto {
    modelFields.forEach { it.applyFrom(this, dsl = dsl) }
    return this
  }

  protected fun mapFields(vararg fields: ModelMetaField<E, *>, dsl: ModelMetaField<E, *>.() -> Unit) {
    fields.forEach { dsl.invoke(it) }
  }
}
