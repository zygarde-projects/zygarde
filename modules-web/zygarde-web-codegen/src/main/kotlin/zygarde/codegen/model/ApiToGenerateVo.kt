package zygarde.codegen.model

data class ApiToGenerateVo(
  var apiInterfacePackage: String,
  var controllerPackage: String,
  var serviceInterfacePackage: String,
  var apiName: String,
  var basePath: String? = null,
  var functions: MutableList<ApiFunctionToGenerateVo> = mutableListOf(),
  var separateFeign: Boolean = true,
  var feignUrlProperty: String? = null,
  var serviceImplPackage: String = "zygarde.generated.service.impl",
  var crudServiceImpls: List<CrudServiceImplToGenerateVo> = emptyList(),
)
