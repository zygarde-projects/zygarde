package zygarde.codegen.meta

data class CodegenSealedInterface(
  val name: String,
  val discriminatorProperty: String = "type",
  val subtypes: List<SealedSubtypeMapping> = emptyList(),
)

data class SealedSubtypeMapping(
  val discriminatorValue: String,
  val dto: CodegenDto,
)
