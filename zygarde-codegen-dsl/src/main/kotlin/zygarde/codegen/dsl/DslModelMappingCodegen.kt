package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.ModelToDtoFieldMappingVo
import zygarde.codegen.dsl.model.type.ForceNull
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
    forceNull: ForceNull,
    vararg dtos: CodegenDto,
  ) {
    mapToDtos("", forceNull, *dtos)
  }

  protected fun ModelMetaField<*, *>.mapToDtos(
    comment: String,
    vararg dtos: CodegenDto,
  ) {
    mapToDtos(
      comment = comment,
      forceNull = ForceNull.NONE,
      *dtos
    )
  }

  protected fun ModelMetaField<*, *>.mapToDtos(
    comment: String,
    forceNull: ForceNull,
    vararg dtos: CodegenDto,
  ) {
    mapToDtos(
      comment = comment,
      dtoRef = null,
      dtoRefClass = null,
      refCollection = false,
      forceNull = forceNull,
      *dtos
    )
  }

  protected fun ModelMetaField<*, *>.mapToDtos(
    comment: String = "",
    dtoRef: CodegenDto? = null,
    dtoRefClass: Class<*>? = null,
    refCollection: Boolean = false,
    forceNull: ForceNull = ForceNull.NONE,
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
          forceNull = forceNull,
        )
      )
    }
  }
}
