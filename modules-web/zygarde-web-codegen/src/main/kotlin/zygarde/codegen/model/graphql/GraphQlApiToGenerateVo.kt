package zygarde.codegen.model.graphql

data class GraphQlApiToGenerateVo(
  var controllerPackage: String,
  var serviceInterfacePackage: String,
  var apiName: String,
  var functions: MutableList<GraphQlFunctionToGenerateVo> = mutableListOf(),
  var typeDefinitions: MutableList<GraphQlTypeDefinitionToGenerateVo> = mutableListOf(),
  var lazyTypes: MutableList<GraphQlLazyTypeToGenerateVo> = mutableListOf(),
)
