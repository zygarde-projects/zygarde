package zygarde.codegen.model

data class ApiVo(
  var apiInterfacePackage: String,
  var controllerPackage: String,
  var serviceInterfacePackage: String,
  var apiName: String,
  var basePath: String? = null,
  var functions: List<ApiFunctionVo>,
)
