package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlDescription
import zygarde.codegen.model.graphql.requireGraphQlName
import zygarde.codegen.model.graphql.requireUniqueGraphQlName

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
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    val typeDefinition = DslGraphQlTypeDefinition.type(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  fun input(name: String, dsl: DslGraphQlTypeDefinition.() -> Unit) {
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    val typeDefinition = DslGraphQlTypeDefinition.input(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  fun enumType(name: String, dsl: DslGraphQlTypeDefinition.() -> Unit) {
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    val typeDefinition = DslGraphQlTypeDefinition.enumType(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  inline fun <reified T : Enum<T>> enumType(name: String = T::class.defaultGraphQlType()) {
    enumType(name) {
      values<T>()
    }
  }

  fun scalar(name: String, description: String? = null) {
    requireGraphQlName(name, "GraphQL type definition name")
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    requireGraphQlDescription(description, "GraphQL scalar '$name'")
    typeDefinitions.add(
      GraphQlTypeDefinitionToGenerateVo(
        kind = GraphQlTypeDefinitionKind.SCALAR,
        name = name,
        description = description,
      )
    )
  }

  inline fun <reified T : Any> scalar(description: String? = null) {
    scalar(T::class.defaultGraphQlType(), description)
  }

  fun union(name: String, vararg memberTypes: String, description: String? = null) {
    union(name, memberTypes.asIterable(), description)
  }

  fun union(name: String, memberTypes: Iterable<String>, description: String? = null) {
    requireGraphQlName(name, "GraphQL type definition name")
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    requireGraphQlDescription(description, "GraphQL union '$name'")
    val memberList = memberTypes.toMutableList()
    require(memberList.isNotEmpty()) {
      "GraphQL union '$name' must declare at least one member type"
    }
    memberList.forEach { requireGraphQlName(it, "GraphQL union '$name' member type") }
    val seen = mutableSetOf<String>()
    memberList.firstOrNull { !seen.add(it) }?.let { duplicate ->
      throw IllegalArgumentException("GraphQL union '$name' member type '$duplicate' is already declared")
    }
    typeDefinitions.add(
      GraphQlTypeDefinitionToGenerateVo(
        kind = GraphQlTypeDefinitionKind.UNION,
        name = name,
        unionMemberTypes = memberList,
        description = description,
      )
    )
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
    requireUniqueGraphQlName(
      functionName,
      functions.filter { it.operation == operation }.map { it.functionName },
      "GraphQL ${operation.name.lowercase()} field"
    )
    val dslFunction = DslGraphQlFunction(functionName, operation).also(dsl)
    functions.add(dslFunction.toGraphQlFunctionToGenerateVo())
  }
}
