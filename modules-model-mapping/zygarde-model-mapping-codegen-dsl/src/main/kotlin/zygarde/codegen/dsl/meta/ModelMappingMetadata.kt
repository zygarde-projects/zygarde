package zygarde.codegen.dsl.meta

import zygarde.codegen.meta.CodegenDto

/**
 * The resolved field structure of every model-mapping DTO collected from one or
 * more [zygarde.codegen.dsl.ModelMappingDslCodegen] runs. Downstream generators
 * (notably GraphQL SDL derivation) look DTOs up here instead of re-declaring
 * their fields by hand.
 */
class ModelMappingMetadata(
  private val fieldsByDto: Map<CodegenDto, List<ResolvedDtoField>>,
) {
  val dtos: Set<CodegenDto> get() = fieldsByDto.keys

  fun fieldsOf(dto: CodegenDto): List<ResolvedDtoField>? = fieldsByDto[dto]

  operator fun contains(dto: CodegenDto): Boolean = dto in fieldsByDto

  companion object {
    val EMPTY = ModelMappingMetadata(emptyMap())
  }
}
