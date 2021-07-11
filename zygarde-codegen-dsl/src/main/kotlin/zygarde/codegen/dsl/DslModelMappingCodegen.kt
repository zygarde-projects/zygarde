package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.ModelToDtoFieldMappingVo
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import kotlin.reflect.KClass

abstract class DslModelMappingCodegen<E : Any>(val modelClass: KClass<E>) {

  val modelFieldToDtoMappings = mutableListOf<ModelToDtoFieldMappingVo>()

  fun execte() {
    codegen()
  }

  protected abstract fun codegen()

  protected fun ModelMetaField<*, *>.mapToDtos(
    vararg dtos: CodegenDto,
    dsl: (ModelToDtoFieldMappingVo.() -> Unit)? = null
  ) {
    dtos.forEach { dto ->
      ModelToDtoFieldMappingVo(this, dto)
        .also {
          dsl?.invoke(it)
        }
        .also(modelFieldToDtoMappings::add)
    }
  }

  protected inline fun <reified F : Any> extraField(
    fieldName: String,
    nullable: Boolean = false,
  ): ModelMetaField<E, F> {
    return ModelMetaField(modelClass, fieldName, F::class, nullable, true)
  }

  protected inline fun <reified F : Any> extraCollectionField(
    fieldName: String,
    nullable: Boolean = false,
  ): ModelMetaField<E, Collection<*>> {
    return ModelMetaField(modelClass, fieldName, Collection::class, nullable, true, arrayOf(F::class))
  }
}
