package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.model.graphql.GraphQlArgumentToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import kotlin.reflect.KClass

class DslGraphQlFunction(
  val functionName: String,
  val operation: GraphQlOperation,
) {
  var serviceName: String? = null
  var serviceFunctionName: String? = null
  private val arguments: MutableList<GraphQlArgumentToGenerateVo> = mutableListOf()
  private var responseType: TypeName? = null
  private var responseGraphQlType: String? = null
  private var responseCollection: Boolean = false

  fun argument(name: String, type: KClass<*>, graphQlType: String = type.defaultGraphQlType(), nullable: Boolean = false) {
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type.asTypeName(),
        graphQlType = graphQlType,
        nullable = nullable,
      )
    )
  }

  fun argument(name: String, type: TypeName, graphQlType: String, nullable: Boolean = false) {
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type,
        graphQlType = graphQlType,
        nullable = nullable,
      )
    )
  }

  inline fun <reified T : Any> argument(name: String, graphQlType: String = T::class.defaultGraphQlType(), nullable: Boolean = false) {
    argument(name, T::class, graphQlType, nullable)
  }

  fun returns(type: KClass<*>, graphQlType: String = type.defaultGraphQlType()) {
    responseType = type.asTypeName()
    responseGraphQlType = graphQlType
    responseCollection = false
  }

  fun returns(type: TypeName, graphQlType: String) {
    responseType = type
    responseGraphQlType = graphQlType
    responseCollection = false
  }

  inline fun <reified T : Any> returns(graphQlType: String = T::class.defaultGraphQlType()) {
    returns(T::class, graphQlType)
  }

  fun returnsCollection(type: KClass<*>, graphQlType: String = type.defaultGraphQlType()) {
    responseType = type.asTypeName()
    responseGraphQlType = graphQlType
    responseCollection = true
  }

  fun returnsCollection(type: TypeName, graphQlType: String) {
    responseType = type
    responseGraphQlType = graphQlType
    responseCollection = true
  }

  inline fun <reified T : Any> returnsCollection(graphQlType: String = T::class.defaultGraphQlType()) {
    returnsCollection(T::class, graphQlType)
  }

  fun toGraphQlFunctionToGenerateVo(): GraphQlFunctionToGenerateVo {
    return GraphQlFunctionToGenerateVo(
      operation = operation,
      functionName = functionName,
      arguments = arguments,
      responseType = requireNotNull(responseType) { "GraphQL function $functionName must declare a response type" },
      responseGraphQlType = requireNotNull(responseGraphQlType) { "GraphQL function $functionName must declare a GraphQL response type" },
      responseCollection = responseCollection,
      serviceName = serviceName,
      serviceFunctionName = serviceFunctionName ?: functionName,
    )
  }
}

fun KClass<*>.defaultGraphQlType(): String {
  return when (this) {
    Int::class -> "Int"
    Long::class -> "Long"
    String::class -> "String"
    Boolean::class -> "Boolean"
    Float::class -> "Float"
    Double::class -> "Float"
    else -> simpleName ?: error("GraphQL type name must be provided for $this")
  }
}
