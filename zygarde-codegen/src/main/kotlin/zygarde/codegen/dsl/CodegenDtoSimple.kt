package zygarde.codegen.dsl

interface CodegenDtoSimple : CodegenDto {
  override val name: String
  override val superClass: String?
    get() = null
}
