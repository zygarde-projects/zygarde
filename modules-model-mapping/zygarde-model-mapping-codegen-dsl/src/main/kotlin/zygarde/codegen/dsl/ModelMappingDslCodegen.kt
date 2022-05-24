package zygarde.codegen.dsl

import zygarde.codegen.dsl.model.internal.DtoFieldMapping

abstract class ModelMappingDslCodegen {
  val dtoFieldMappings = mutableListOf<DtoFieldMapping>()

  open fun execute() {}
}
