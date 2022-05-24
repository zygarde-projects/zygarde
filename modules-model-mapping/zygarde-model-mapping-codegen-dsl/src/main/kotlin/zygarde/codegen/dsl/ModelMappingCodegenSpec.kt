package zygarde.codegen.dsl

import zygarde.codegen.meta.CodegenDto

abstract class ModelMappingCodegenSpec(buildMapping: ModelMappingCodegenSpec.() -> Unit) : ModelMappingDslCodegen() {
  init {
    buildMapping()
  }

  operator fun CodegenDto.invoke(mapping: ModelMappingSpec.() -> Unit = {}) {
    mapping.invoke(ModelMappingSpec(this, super.dtoFieldMappings))
  }
}
