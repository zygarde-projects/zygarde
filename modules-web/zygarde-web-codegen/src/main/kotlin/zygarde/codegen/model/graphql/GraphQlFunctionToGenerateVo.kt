package zygarde.codegen.model.graphql

import com.squareup.kotlinpoet.TypeName

data class GraphQlFunctionToGenerateVo(
  var operation: GraphQlOperation,
  var functionName: String,
  var arguments: MutableList<GraphQlArgumentToGenerateVo> = mutableListOf(),
  var responseType: TypeName,
  var responseGraphQlType: String,
  var responseCollection: Boolean = false,
  var responseNullable: Boolean = false,
  var responseItemNullable: Boolean = false,
  var serviceName: String? = null,
  var serviceFunctionName: String? = null,
  var description: String? = null,
)

data class GraphQlArgumentToGenerateVo(
  var name: String,
  var type: TypeName,
  var graphQlType: String,
  var nullable: Boolean = false,
  var collection: Boolean = false,
  var itemNullable: Boolean = false,
  var defaultValue: String? = null,
)

data class GraphQlTypeDefinitionToGenerateVo(
  var kind: GraphQlTypeDefinitionKind,
  var name: String,
  var fields: MutableList<GraphQlFieldToGenerateVo> = mutableListOf(),
  var enumValues: MutableList<String> = mutableListOf(),
  var description: String? = null,
)

data class GraphQlFieldToGenerateVo(
  var name: String,
  var graphQlType: String,
  var nullable: Boolean = false,
  var collection: Boolean = false,
  var itemNullable: Boolean = false,
  var defaultValue: String? = null,
)

enum class GraphQlOperation {
  QUERY,
  MUTATION,
  SUBSCRIPTION,
}

enum class GraphQlTypeDefinitionKind {
  TYPE,
  INPUT,
  ENUM,
  SCALAR,
}
