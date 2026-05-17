package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.model.graphql.GraphQlArgumentToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.requireGraphQlDeprecationReason
import zygarde.codegen.model.graphql.requireGraphQlDescription
import zygarde.codegen.model.graphql.requireGraphQlName
import zygarde.codegen.model.graphql.requireUniqueGraphQlName
import kotlin.reflect.KClass

class DslGraphQlFunction(
  val functionName: String,
  val operation: GraphQlOperation,
  private val graphQlTypeResolver: (KClass<*>) -> String = { it.defaultGraphQlType() },
) {
  var serviceName: String? = null
  var serviceFunctionName: String? = null
  var description: String? = null
  var deprecationReason: String? = null
  private val arguments: MutableList<GraphQlArgumentToGenerateVo> = mutableListOf()
  private var responseType: TypeName? = null
  private var responseGraphQlType: String? = null
  private var responseCollection: Boolean = false
  private var responseNullable: Boolean = false
  private var responseItemNullable: Boolean = false

  init {
    requireGraphQlName(functionName, "GraphQL ${operation.name.lowercase()} field")
  }

  fun argument(
    name: String,
    type: KClass<*>,
    graphQlType: String = graphQlTypeOf(type),
    nullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    requireGraphQlName(name, "GraphQL argument name")
    requireGraphQlName(graphQlType, "GraphQL argument type")
    requireUniqueGraphQlName(name, arguments.map { it.name }, "GraphQL argument")
    requireArgumentDescription(name, description)
    requireArgumentDeprecation(name, nullable, defaultValue, deprecationReason)
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type.asTypeName(),
        graphQlType = graphQlType,
        nullable = nullable,
        defaultValue = defaultValue,
        description = description,
        deprecationReason = deprecationReason,
      )
    )
  }

  fun argument(
    name: String,
    type: TypeName,
    graphQlType: String,
    nullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    requireGraphQlName(name, "GraphQL argument name")
    requireGraphQlName(graphQlType, "GraphQL argument type")
    requireUniqueGraphQlName(name, arguments.map { it.name }, "GraphQL argument")
    requireArgumentDescription(name, description)
    requireArgumentDeprecation(name, nullable, defaultValue, deprecationReason)
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type,
        graphQlType = graphQlType,
        nullable = nullable,
        defaultValue = defaultValue,
        description = description,
        deprecationReason = deprecationReason,
      )
    )
  }

  inline fun <reified T : Any> argument(
    name: String,
    graphQlType: String = graphQlTypeOf(T::class),
    nullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    argument(name, T::class, graphQlType, nullable, defaultValue, description, deprecationReason)
  }

  fun collectionArgument(
    name: String,
    type: KClass<*>,
    graphQlType: String = graphQlTypeOf(type),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    requireGraphQlName(name, "GraphQL argument name")
    requireGraphQlName(graphQlType, "GraphQL argument type")
    requireUniqueGraphQlName(name, arguments.map { it.name }, "GraphQL argument")
    requireArgumentDescription(name, description)
    requireArgumentDeprecation(name, nullable, defaultValue, deprecationReason)
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type.asTypeName(),
        graphQlType = graphQlType,
        nullable = nullable,
        collection = true,
        itemNullable = itemNullable,
        defaultValue = defaultValue,
        description = description,
        deprecationReason = deprecationReason,
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
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    requireGraphQlName(name, "GraphQL argument name")
    requireGraphQlName(graphQlType, "GraphQL argument type")
    requireUniqueGraphQlName(name, arguments.map { it.name }, "GraphQL argument")
    requireArgumentDescription(name, description)
    requireArgumentDeprecation(name, nullable, defaultValue, deprecationReason)
    arguments.add(
      GraphQlArgumentToGenerateVo(
        name = name,
        type = type,
        graphQlType = graphQlType,
        nullable = nullable,
        collection = true,
        itemNullable = itemNullable,
        defaultValue = defaultValue,
        description = description,
        deprecationReason = deprecationReason,
      )
    )
  }

  inline fun <reified T : Any> collectionArgument(
    name: String,
    graphQlType: String = graphQlTypeOf(T::class),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    collectionArgument(name, T::class, graphQlType, nullable, itemNullable, defaultValue, description, deprecationReason)
  }

  fun returns(type: KClass<*>, graphQlType: String = graphQlTypeOf(type), nullable: Boolean = false) {
    requireGraphQlName(graphQlType, "GraphQL response type")
    responseType = type.asTypeName()
    responseGraphQlType = graphQlType
    responseCollection = false
    responseNullable = nullable
    responseItemNullable = false
  }

  fun returns(type: TypeName, graphQlType: String, nullable: Boolean = false) {
    requireGraphQlName(graphQlType, "GraphQL response type")
    responseType = type
    responseGraphQlType = graphQlType
    responseCollection = false
    responseNullable = nullable
    responseItemNullable = false
  }

  inline fun <reified T : Any> returns(graphQlType: String = graphQlTypeOf(T::class), nullable: Boolean = false) {
    returns(T::class, graphQlType, nullable)
  }

  fun returnsCollection(
    type: KClass<*>,
    graphQlType: String = graphQlTypeOf(type),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
  ) {
    requireGraphQlName(graphQlType, "GraphQL response type")
    responseType = type.asTypeName()
    responseGraphQlType = graphQlType
    responseCollection = true
    responseNullable = nullable
    responseItemNullable = itemNullable
  }

  fun returnsCollection(type: TypeName, graphQlType: String, nullable: Boolean = false, itemNullable: Boolean = false) {
    requireGraphQlName(graphQlType, "GraphQL response type")
    responseType = type
    responseGraphQlType = graphQlType
    responseCollection = true
    responseNullable = nullable
    responseItemNullable = itemNullable
  }

  inline fun <reified T : Any> returnsCollection(
    graphQlType: String = graphQlTypeOf(T::class),
    nullable: Boolean = false,
    itemNullable: Boolean = false,
  ) {
    returnsCollection(T::class, graphQlType, nullable, itemNullable)
  }

  @PublishedApi
  internal fun graphQlTypeOf(type: KClass<*>): String = graphQlTypeResolver(type)

  private fun requireArgumentDescription(name: String, description: String?) {
    requireGraphQlDescription(description, "GraphQL ${operation.name.lowercase()} field '$functionName' argument '$name'")
  }

  private fun requireArgumentDeprecation(
    name: String,
    nullable: Boolean,
    defaultValue: String?,
    deprecationReason: String?,
  ) {
    val argumentLabel = "GraphQL ${operation.name.lowercase()} field '$functionName' argument '$name'"
    requireGraphQlDeprecationReason(deprecationReason, argumentLabel)
    require(deprecationReason == null || nullable || defaultValue != null) {
      "$argumentLabel cannot be deprecated because it is a required argument"
    }
  }

  fun toGraphQlFunctionToGenerateVo(): GraphQlFunctionToGenerateVo {
    val fieldLabel = "GraphQL ${operation.name.lowercase()} field '$functionName'"
    requireGraphQlDescription(description, fieldLabel)
    requireGraphQlDeprecationReason(deprecationReason, fieldLabel)
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
      serviceFunctionName = serviceFunctionName,
      description = description,
      deprecationReason = deprecationReason,
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
