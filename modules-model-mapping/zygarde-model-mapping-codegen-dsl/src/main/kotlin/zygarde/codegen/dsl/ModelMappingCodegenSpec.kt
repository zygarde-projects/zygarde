package zygarde.codegen.dsl

import zygarde.codegen.meta.CodegenDto

abstract class ModelMappingCodegenSpec(buildMapping: ModelMappingCodegenSpec.() -> Unit) : ModelMappingDslCodegen() {

  companion object {
    val groupInvokingDto: ThreadLocal<CodegenDto?> = ThreadLocal.withInitial { null }
  }

  init {
    buildMapping()
  }

  operator fun CodegenDto.invoke(mapping: ModelMappingSpec.() -> Unit = {}) {
    val groupInvoking = groupInvokingDto.get()
    if (groupInvoking == null || groupInvoking == this) {
      mapping.invoke(ModelMappingSpec(dto = this, dtoFieldMappings = super.dtoFieldMappings))
    }
  }

  fun group(vararg dtos: CodegenDto, mapping: ModelMappingSpec.() -> Unit = {}) {
    dtos.forEach { dto ->
      try {
        groupInvokingDto.set(dto)
        dto.invoke(mapping)
      } finally {
        groupInvokingDto.remove()
      }
    }
  }
}
