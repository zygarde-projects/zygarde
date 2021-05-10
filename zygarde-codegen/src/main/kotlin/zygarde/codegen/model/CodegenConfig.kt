package zygarde.codegen.model

data class CodegenConfig(
  val basePackageName: String = "zygarde.codegen",
) {

  fun dtoNameToClass(dtoName: String): String {
    return "$basePackageName.dto.$dtoName"
  }
}
