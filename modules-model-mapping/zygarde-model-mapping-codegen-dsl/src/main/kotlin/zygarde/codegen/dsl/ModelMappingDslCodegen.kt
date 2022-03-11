package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

abstract class ModelMappingDslCodegen<E : Any>(val modelClass: KClass<E>) {

  val dtoFieldMappings = mutableListOf<DtoFieldMapping>()

  fun execute() {
    codegen()
  }

  protected abstract fun codegen()

  protected fun mapToDto(dto: CodegenDto, dsl: ModelMappingToDtoDsl<E>.() -> Unit) {
    dsl.invoke(ModelMappingToDtoDsl(modelClass, dtoFieldMappings, dto))
  }

  protected fun applyFromDto(dto: CodegenDto, dsl: DtoApplyToModelDsl<E>.() -> Unit) {
    dsl.invoke(DtoApplyToModelDsl(modelClass, dtoFieldMappings, dto))
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
}
