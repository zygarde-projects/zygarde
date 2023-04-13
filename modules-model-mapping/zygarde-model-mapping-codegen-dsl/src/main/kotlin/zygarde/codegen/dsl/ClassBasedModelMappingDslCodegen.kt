package zygarde.codegen.dsl

import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

abstract class ClassBasedModelMappingDslCodegen<E : Any>(val modelClass: KClass<E>) : ModelMappingDslCodegen() {

  override fun execute() {
    codegen()
  }

  protected abstract fun codegen()

  protected fun dto(dto: CodegenDto, dsl: ModelToDtoDsl<E>.() -> Unit) {
    val modelToDtoDsl = ModelToDtoDsl(modelClass, dto)
    dsl.invoke(modelToDtoDsl)
    val fieldMappings = modelToDtoDsl.dtoFieldMappings
    if (fieldMappings.map { it.modelField.modelClass }.toSet().size > 1) {
      fieldMappings.forEach { it.compound = true }
    }
    dtoFieldMappings.addAll(fieldMappings)
  }

  protected fun req(dto: CodegenDto, dsl: DtoApplyToModelDsl<E>.() -> Unit) {
    val dtoApplyToModelDsl = DtoApplyToModelDsl(modelClass, dto)
    dsl.invoke(dtoApplyToModelDsl)
    dtoFieldMappings.addAll(dtoApplyToModelDsl.dtoFieldMappings)
  }

  protected inline fun <reified F : Any> custom(
    fieldName: String,
    nullable: Boolean = false,
    dsl: (ModelMetaField<E, F>.() -> Unit) = {},
  ): ModelMetaField<E, F> {
    return ModelMetaField(modelClass, fieldName, F::class, nullable, true)
      .also(dsl)
  }

  protected inline fun <reified F : Any> collection(
    fieldName: String,
    nullable: Boolean = false,
    dsl: (ModelMetaField<E, Collection<*>>.() -> Unit) = {},
  ): ModelMetaField<E, Collection<*>> {
    return ModelMetaField(modelClass, fieldName, Collection::class, nullable, true, arrayOf(F::class.java))
      .also(dsl)
  }
}
