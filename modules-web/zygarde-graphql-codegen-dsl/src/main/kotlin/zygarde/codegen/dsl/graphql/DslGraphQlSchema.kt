package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlName

class DslGraphQlSchema(
  private val config: GraphQlDslCodegenConfig,
  private val schemaName: String,
) {
  private val functions: MutableList<GraphQlFunctionToGenerateVo> = mutableListOf()
  private val typeDefinitions: MutableList<GraphQlTypeDefinitionToGenerateVo> = mutableListOf()

  fun query(functionName: String, dsl: DslGraphQlFunction.() -> Unit) {
    buildForOperation(functionName, GraphQlOperation.QUERY, dsl)
  }

  fun mutation(functionName: String, dsl: DslGraphQlFunction.() -> Unit) {
    buildForOperation(functionName, GraphQlOperation.MUTATION, dsl)
  }

  fun subscription(functionName: String, dsl: DslGraphQlFunction.() -> Unit) {
    buildForOperation(functionName, GraphQlOperation.SUBSCRIPTION, dsl)
  }

  fun type(name: String, dsl: DslGraphQlTypeDefinition.() -> Unit) {
    val typeDefinition = DslGraphQlTypeDefinition.type(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  fun input(name: String, dsl: DslGraphQlTypeDefinition.() -> Unit) {
    val typeDefinition = DslGraphQlTypeDefinition.input(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  fun enumType(name: String, dsl: DslGraphQlTypeDefinition.() -> Unit) {
    val typeDefinition = DslGraphQlTypeDefinition.enumType(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  inline fun <reified T : Enum<T>> enumType(name: String = T::class.defaultGraphQlType()) {
    enumType(name) {
      values<T>()
    }
  }

  fun scalar(name: String) {
    requireGraphQlName(name, "GraphQL type definition name")
    typeDefinitions.add(
      GraphQlTypeDefinitionToGenerateVo(
        kind = GraphQlTypeDefinitionKind.SCALAR,
        name = name,
      )
    )
  }

  inline fun <reified T : Any> scalar() {
    scalar(T::class.defaultGraphQlType())
  }

  fun toGraphQlApiToGenerateVo(): GraphQlApiToGenerateVo {
    return GraphQlApiToGenerateVo(
      controllerPackage = config.controllerPackage,
      serviceInterfacePackage = config.serviceInterfacePackage,
      apiName = schemaName,
      functions = functions,
      typeDefinitions = typeDefinitions,
    )
  }

  private fun buildForOperation(
    functionName: String,
    operation: GraphQlOperation,
    dsl: DslGraphQlFunction.() -> Unit
  ) {
    val dslFunction = DslGraphQlFunction(functionName, operation).also(dsl)
    functions.add(dslFunction.toGraphQlFunctionToGenerateVo())
  }
}
