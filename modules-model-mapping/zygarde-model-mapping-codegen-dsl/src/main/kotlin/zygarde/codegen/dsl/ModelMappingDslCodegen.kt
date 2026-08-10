package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoSortableFieldPath
import zygarde.codegen.meta.CodegenSealedInterface

abstract class ModelMappingDslCodegen {
  val dtoFieldMappings = mutableListOf<DtoFieldMapping>()
  val dtoSortableFieldPaths = mutableListOf<DtoSortableFieldPath>()
  val sealedInterfaces = mutableListOf<CodegenSealedInterface>()

  open fun execute() {}
}
