package zygarde.codegen.dsl

interface DtoDsl {
  val name: String
  fun superClasses(): Array<out String>
}
