package zygarde.codegen.dsl.model.internal

import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField

data class ModelToDtoFieldMappingVo(
  val modelField: ModelMetaField<*, *>,
  val dto: CodegenDto,
  val comment: String = "",
  val dtoRef: CodegenDto? = null,
  val dtoRefClass: Class<*>? = null,
  val refCollection: Boolean = false
)
