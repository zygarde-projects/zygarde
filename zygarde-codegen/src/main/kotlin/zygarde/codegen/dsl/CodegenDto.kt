package zygarde.codegen.dsl

interface CodegenDto {
  val name: String
  fun superClasses(): Collection<String>
}
