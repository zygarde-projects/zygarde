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
  private var responseNullable: Boolean = false
  private var responseItemNullable: Boolean = false

  fun argument(
    name: String,
    type: KClass<*>,
    graphQlType: String = type.defaultGraphQlType(),
    nullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type.asTypeName(),
        graphQlType = graphQlType,
        nullable = nullable,
        defaultValue = defaultValue,
      )
    )
  }

  fun argument(name: String, type: TypeName, graphQlType: String, nullable: Boolean = false, defaultValue: String? = null) {
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type,
        graphQlType = graphQlType,
        nullable = nullable,
        defaultValue = defaultValue,
      )
    )
  }

  inline fun <reified T : Any> argument(
    name: String,
    graphQlType: String = T::class.defaultGraphQlType(),
    nullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    argument(name, T::class, graphQlType, nullable, defaultValue)
  }

  fun collectionArgument(
    name: String,
    type: KClass<*>,
    graphQlType: String = type.defaultGraphQlType(),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type.asTypeName(),
        graphQlType = graphQlType,
        nullable = nullable,
        collection = true,
        itemNullable = itemNullable,
        defaultValue = defaultValue,
      )
    )
  }

  fun collectionArgument(
    name: String,
    type: TypeName,
    graphQlType: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type,
        graphQlType = graphQlType,
        nullable = nullable,
        collection = true,
        itemNullable = itemNullable,
        defaultValue = defaultValue,
      )
    )
  }

  inline fun <reified T : Any> collectionArgument(
    name: String,
    graphQlType: String = T::class.defaultGraphQlType(),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    collectionArgument(name, T::class, graphQlType, nullable, itemNullable, defaultValue)
  }

  fun returns(type: KClass<*>, graphQlType: String = type.defaultGraphQlType(), nullable: Boolean = false) {
    responseType = type.asTypeName()
    responseGraphQlType = graphQlType
    responseCollection = false
    responseNullable = nullable
    responseItemNullable = false
  }

  fun returns(type: TypeName, graphQlType: String, nullable: Boolean = false) {
    responseType = type
    responseGraphQlType = graphQlType
    responseCollection = false
    responseNullable = nullable
    responseItemNullable = false
  }

  inline fun <reified T : Any> returns(graphQlType: String = T::class.defaultGraphQlType(), nullable: Boolean = false) {
    returns(T::class, graphQlType, nullable)
  }

  fun returnsCollection(
    type: KClass<*>,
    graphQlType: String = type.defaultGraphQlType(),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
  ) {
    responseType = type.asTypeName()
    responseGraphQlType = graphQlType
    responseCollection = true
    responseNullable = nullable
    responseItemNullable = itemNullable
  }

  fun returnsCollection(type: TypeName, graphQlType: String, nullable: Boolean = false, itemNullable: Boolean = false) {
    responseType = type
    responseGraphQlType = graphQlType
    responseCollection = true
    responseNullable = nullable
    responseItemNullable = itemNullable
  }

  inline fun <reified T : Any> returnsCollection(
    graphQlType: String = T::class.defaultGraphQlType(),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
  ) {
    returnsCollection(T::class, graphQlType, nullable, itemNullable)
  }

  fun toGraphQlFunctionToGenerateVo(): GraphQlFunctionToGenerateVo {
    return GraphQlFunctionToGenerateVo(
      operation = operation,
      functionName = functionName,
      arguments = arguments,
      responseType = requireNotNull(responseType) { "GraphQL function $functionName must declare a response type" },
      responseGraphQlType = requireNotNull(responseGraphQlType) { "GraphQL function $functionName must declare a GraphQL response type" },
      responseCollection = responseCollection,
      responseNullable = responseNullable,
      responseItemNullable = responseItemNullable,
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
