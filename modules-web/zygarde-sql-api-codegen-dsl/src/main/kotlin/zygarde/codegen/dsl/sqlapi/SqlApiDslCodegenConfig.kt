package zygarde.codegen.dsl.sqlapi

data class SqlApiDslCodegenConfig(
  val dtoPackage: String,
  val apiInterfacePackage: String,
  val controllerPackage: String,
  val serviceInterfacePackage: String,
  val serviceImplPackage: String,
)
