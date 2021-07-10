package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.ModelToDtoFieldMappingVo
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField

abstract class DslModelMappingCodegen {

  val modelFieldToDtoMappings = mutableListOf<ModelToDtoFieldMappingVo>()

  fun execte() {
    codegen()
  }

  protected abstract fun codegen()

  protected fun ModelMetaField<*, *>.mapToDtos(
    vararg dtos: CodegenDto,
  ) {
    mapToDtos("", *dtos)
  }

  protected fun ModelMetaField<*, *>.mapToDtos(
    comment: String,
    vararg dtos: CodegenDto,
  ) {
    mapToDtos(
      comment = comment,
      dtoRef = null,
      dtoRefClass = null,
      refCollection = false,
      *dtos
    )
  }

  protected fun ModelMetaField<*, *>.mapToDtos(
    comment: String = "",
    dtoRef: CodegenDto? = null,
    dtoRefClass: Class<*>? = null,
    refCollection: Boolean = false,
    vararg dtos: CodegenDto,
  ) {
    dtos.forEach { dto ->
      modelFieldToDtoMappings.add(
        ModelToDtoFieldMappingVo(
          modelField = this,
          dto = dto,
          comment = comment,
          dtoRef = dtoRef,
          dtoRefClass = dtoRefClass,
          refCollection = refCollection,
        )
      )
    }
  }
}
