package zygarde.codegen.dsl

import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.CodegenSealedInterface

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
      mapping.invoke(
        ModelMappingSpec(
          dto = this,
          dtoFieldMappings = super.dtoFieldMappings,
          dtoSortableFieldPaths = super.dtoSortableFieldPaths,
        )
      )
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

  fun sealedInterface(name: String, dsl: SealedInterfaceSpec.() -> Unit): CodegenSealedInterface {
    return SealedInterfaceSpec(name).also(dsl).build().also(sealedInterfaces::add)
  }
}
