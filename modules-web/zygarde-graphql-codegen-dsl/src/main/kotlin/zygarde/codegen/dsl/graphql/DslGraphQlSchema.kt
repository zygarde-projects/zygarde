package zygarde.codegen.dsl.graphql

import zygarde.codegen.dsl.meta.ModelMappingMetadata
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlDescription
import zygarde.codegen.model.graphql.requireGraphQlName
import zygarde.codegen.model.graphql.requireUniqueGraphQlName
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

class DslGraphQlSchema(
  private val config: GraphQlDslCodegenConfig,
  private val schemaName: String,
  private val modelMappingMetadata: ModelMappingMetadata = ModelMappingMetadata.EMPTY,
) {
  private val functions: MutableList<GraphQlFunctionToGenerateVo> = mutableListOf()
  private val typeDefinitions: MutableList<GraphQlTypeDefinitionToGenerateVo> = mutableListOf()

  private val typeMapper: GraphQlTypeMapper = GraphQlTypeMapper()
  private val dtoDeriver: GraphQlDtoDeriver by lazy { GraphQlDtoDeriver(modelMappingMetadata, typeMapper) }
  private val graphQlTypeNamesByKotlinType: MutableMap<KClass<*>, String> = mutableMapOf()

  fun bindGraphQlType(type: KClass<*>, graphQlType: String) {
    registerGraphQlType(type, graphQlType)
  }

  inline fun <reified T : Any> bindGraphQlType(graphQlType: String) {
    bindGraphQlType(T::class, graphQlType)
  }

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

  fun type(type: KClass<*>, name: String = type.defaultGraphQlType(), dsl: DslGraphQlEntityProjectionDefinition.() -> Unit) {
    addEntityProjection(type, name, GraphQlTypeDefinitionKind.TYPE, dsl)
  }

  @JvmName("entityType")
  inline fun <reified T : Any> type(
    name: String = T::class.defaultGraphQlType(),
    noinline dsl: DslGraphQlEntityProjectionDefinition.() -> Unit,
  ) {
    type(T::class, name, dsl)
  }

  fun input(name: String, dsl: DslGraphQlTypeDefinition.() -> Unit) {
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    val typeDefinition = DslGraphQlTypeDefinition.input(name).also(dsl)
    typeDefinitions.add(typeDefinition.toGraphQlTypeDefinitionToGenerateVo())
  }

  fun input(type: KClass<*>, name: String = type.defaultGraphQlType(), dsl: DslGraphQlEntityProjectionDefinition.() -> Unit) {
    addEntityProjection(type, name, GraphQlTypeDefinitionKind.INPUT, dsl)
  }

  @JvmName("entityInput")
  inline fun <reified T : Any> input(
    name: String = T::class.defaultGraphQlType(),
    noinline dsl: DslGraphQlEntityProjectionDefinition.() -> Unit,
  ) {
    input(T::class, name, dsl)
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

  /** Register (or override) the GraphQL scalar name used for [type] while deriving DTO types. */
  fun mapScalar(type: KClass<*>, graphQlType: String) {
    requireGraphQlName(graphQlType, "GraphQL scalar mapping for '${type.qualifiedName ?: type.java.name}'")
    typeMapper.register(type, graphQlType)
  }

  inline fun <reified T : Any> mapScalar(graphQlType: String) {
    mapScalar(T::class, graphQlType)
  }

  /**
   * Derive a GraphQL `type` declaration from a model-mapping DTO, together with
   * every DTO and enum it transitively references. Field types, nullability,
   * collections and descriptions come from the shared model-mapping metadata, so
   * the DTO and its GraphQL type cannot drift.
   */
  fun typeFrom(
    dto: CodegenDto,
    name: String = dto.name,
    description: String? = null,
    exclude: Set<String> = emptySet(),
  ) {
    deriveDtoTypeDefinition(dto, GraphQlTypeDefinitionKind.TYPE, "type", name, description, exclude)
  }

  @JvmName("typedTypeFrom")
  inline fun <reified T : Any> typeFrom(
    dto: CodegenDto,
    name: String = dto.name,
    description: String? = null,
    exclude: Set<String> = emptySet(),
  ) {
    typeFrom(dto, name, description, exclude)
    registerGraphQlType(T::class, name)
  }

  /** Derive a GraphQL `input` declaration from a model-mapping DTO. See [typeFrom]. */
  fun inputFrom(
    dto: CodegenDto,
    name: String = dto.name,
    description: String? = null,
    exclude: Set<String> = emptySet(),
  ) {
    deriveDtoTypeDefinition(dto, GraphQlTypeDefinitionKind.INPUT, "input", name, description, exclude)
  }

  @JvmName("typedInputFrom")
  inline fun <reified T : Any> inputFrom(
    dto: CodegenDto,
    name: String = dto.name,
    description: String? = null,
    exclude: Set<String> = emptySet(),
  ) {
    inputFrom(dto, name, description, exclude)
    registerGraphQlType(T::class, name)
  }

  private fun deriveDtoTypeDefinition(
    dto: CodegenDto,
    kind: GraphQlTypeDefinitionKind,
    keyword: String,
    name: String,
    description: String?,
    exclude: Set<String>,
  ) {
    requireGraphQlName(name, "GraphQL type definition name")
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    requireGraphQlDescription(description, "GraphQL $keyword '$name'")
    val existingNames = typeDefinitions.mapTo(mutableSetOf()) { it.name }
    dtoDeriver
      .derive(dto, kind, name, description, exclude) { it in existingNames }
      .forEach { typeDefinitions.add(it) }
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
    val dslFunction = DslGraphQlFunction(functionName, operation, ::resolveGraphQlType).also(dsl)
    functions.add(dslFunction.toGraphQlFunctionToGenerateVo())
  }

  private fun addEntityProjection(
    type: KClass<*>,
    name: String,
    kind: GraphQlTypeDefinitionKind,
    dsl: DslGraphQlEntityProjectionDefinition.() -> Unit,
  ) {
    requireUniqueGraphQlName(name, typeDefinitions.map { it.name }, "GraphQL type definition")
    registerGraphQlType(type, name)
    val projection = DslGraphQlEntityProjectionDefinition(kind, name, typeMapper, ::resolveReferenceGraphQlType).also(dsl).build()
    typeDefinitions.add(projection.typeDefinition)
    projection.additionalTypeDefinitions.forEach { additionalTypeDefinition ->
      if (typeDefinitions.none { it.name == additionalTypeDefinition.name }) {
        typeDefinitions.add(additionalTypeDefinition)
      }
    }
  }

  @PublishedApi
  internal fun registerGraphQlType(type: KClass<*>, graphQlType: String) {
    requireGraphQlName(graphQlType, "GraphQL type mapping for '${type.qualifiedName ?: type.java.name}'")
    val existing = graphQlTypeNamesByKotlinType[type]
    require(existing == null || existing == graphQlType) {
      "Kotlin type '${type.qualifiedName ?: type.java.name}' is already mapped to GraphQL type '$existing'; cannot also map it to '$graphQlType'"
    }
    graphQlTypeNamesByKotlinType[type] = graphQlType
  }

  private fun resolveGraphQlType(type: KClass<*>): String = graphQlTypeNamesByKotlinType[type] ?: type.defaultGraphQlType()

  private fun resolveReferenceGraphQlType(type: KClass<*>, fieldName: String): String {
    return graphQlTypeNamesByKotlinType[type]
      ?: throw IllegalArgumentException(
        "GraphQL entity projection reference field '$fieldName' requires a GraphQL type binding for Kotlin type " +
          "'${type.qualifiedName ?: type.java.name}'. Declare type<${type.simpleName ?: "T"}>(...), " +
          "input<${type.simpleName ?: "T"}>(...), bindGraphQlType<${type.simpleName ?: "T"}>(\"Name\"), " +
          "or pass graphQlType explicitly.",
      )
  }
}
