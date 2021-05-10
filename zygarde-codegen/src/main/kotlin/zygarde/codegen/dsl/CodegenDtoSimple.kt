package zygarde.codegen.dsl

interface CodegenDtoSimple : CodegenDto {
  override val name: String
  override fun superClasses(): Collection<String> = emptyList()
}
