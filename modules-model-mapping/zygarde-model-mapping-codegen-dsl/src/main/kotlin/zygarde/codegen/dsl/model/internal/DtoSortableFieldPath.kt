package zygarde.codegen.dsl.model.internal

import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField

data class DtoSortableFieldPath(
  val dto: CodegenDto,
  val segments: List<ModelMetaField>,
) {
  init {
    require(segments.isNotEmpty()) { "Sortable field path must contain at least one segment." }
  }

  val path: String = segments.joinToString(".") { it.fieldName }

  val rootModelClass = segments.first().modelClass
}
