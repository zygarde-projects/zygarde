package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.meta.CodegenSealedInterface

abstract class ModelMappingDslCodegen {
  val dtoFieldMappings = mutableListOf<DtoFieldMapping>()
  val sealedInterfaces = mutableListOf<CodegenSealedInterface>()

  open fun execute() {}
}
