package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import zygarde.codegen.dsl.meta.ModelMappingMetadata
import zygarde.codegen.dsl.meta.ResolvedDtoField
import zygarde.codegen.dsl.meta.ResolvedDtoProviderField
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.model.graphql.GraphQlEnumValueToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlLazyProviderToGenerateVo
import zygarde.codegen.model.graphql.GraphQlLazySourceFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlLazyTypeToGenerateVo
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlValueProviderParameterType
import zygarde.codegen.model.graphql.requireGraphQlName

/**
 * Derives GraphQL `type` / `input` declarations (plus every DTO and enum they
 * transitively reference) from model-mapping [ModelMappingMetadata], so GraphQL
 * schemas no longer have to re-declare DTO fields by hand.
 *
 * One instance is shared across a single schema: the [dtoGraphQlNames] registry
 * keeps a DTO's GraphQL type name stable whether it is derived explicitly or
 * pulled in transitively as a reference.
 */
internal class GraphQlDtoDeriver(
  private val metadata: ModelMappingMetadata,
  private val typeMapper: GraphQlTypeMapper,
) {
  private val dtoGraphQlNames = mutableMapOf<CodegenDto, String>()

  data class DeriveResult(
    val typeDefinitions: List<GraphQlTypeDefinitionToGenerateVo>,
    val lazyType: GraphQlLazyTypeToGenerateVo?,
  )

  private data class DeriveRequest(
    val dto: CodegenDto,
    val kind: GraphQlTypeDefinitionKind,
    val name: String,
    val description: String?,
    val exclude: Set<String>,
    val lazyProviders: GraphQlLazyProvidersConfig?,
    val sourceType: ClassName?,
    val sourceAssemblerType: ClassName?,
  )

  private class BuiltType(
    val type: GraphQlTypeDefinitionToGenerateVo,
    val referencedDtos: List<CodegenDto>,
    val enums: List<GraphQlTypeDefinitionToGenerateVo>,
  )

  /**
   * Derive [dto] and every DTO / enum it transitively references into GraphQL
   * type definitions. [alreadyDeclared] reports type names already present in the
   * schema so existing (manual or previously-derived) declarations are reused
   * rather than emitted twice. Returns the new definitions in dependency order.
   */
  fun derive(
    dto: CodegenDto,
    kind: GraphQlTypeDefinitionKind,
    name: String,
    description: String?,
    exclude: Set<String>,
    lazyProviders: GraphQlLazyProvidersConfig?,
    sourceType: ClassName?,
    sourceAssemblerType: ClassName?,
    alreadyDeclared: (String) -> Boolean,
  ): DeriveResult {
    require(kind == GraphQlTypeDefinitionKind.TYPE || kind == GraphQlTypeDefinitionKind.INPUT) {
      "GraphQL DTO derivation supports only 'type' and 'input' kinds"
    }
    registerName(dto, name)
    val result = mutableListOf<GraphQlTypeDefinitionToGenerateVo>()
    val producedNames = mutableSetOf<String>()
    val queue = ArrayDeque<DeriveRequest>()
    queue.add(DeriveRequest(dto, kind, name, description, exclude, lazyProviders, sourceType, sourceAssemblerType))
    var lazyType: GraphQlLazyTypeToGenerateVo? = null
    while (queue.isNotEmpty()) {
      val request = queue.removeFirst()
      if (request.name in producedNames || alreadyDeclared(request.name)) {
        continue
      }
      val built = buildType(request)
      result.add(built.type)
      if (request.dto == dto) {
        lazyType = buildLazyType(request, built.type.fields)
      }
      producedNames.add(request.name)
      built.enums.forEach { enum ->
        if (enum.name !in producedNames && !alreadyDeclared(enum.name)) {
          result.add(enum)
          producedNames.add(enum.name)
        }
      }
      built.referencedDtos.forEach { refDto ->
        val refName = graphQlNameOf(refDto)
        if (refName !in producedNames && !alreadyDeclared(refName)) {
          queue.add(DeriveRequest(refDto, request.kind, refName, null, emptySet(), null, null, null))
        }
      }
    }
    return DeriveResult(typeDefinitions = result, lazyType = lazyType)
  }

  private fun buildType(request: DeriveRequest): BuiltType {
    requireGraphQlName(request.name, "GraphQL ${request.kind.keyword()} name")
    val fields = metadata.fieldsOf(request.dto)
      ?: throw IllegalArgumentException(
        "GraphQL derivation: DTO '${request.dto.name}' is not part of model-mapping metadata. " +
          "Ensure its ModelMappingCodegenSpec is on the codegen classpath.",
      )
    val referencedDtos = mutableListOf<CodegenDto>()
    val enums = mutableListOf<GraphQlTypeDefinitionToGenerateVo>()
    val lazyProviderOverrides = request.lazyProviders?.overrides.orEmpty()
    val graphQlFields = fields
      .filterNot { it.name in request.exclude }
      .map { field ->
        requireGraphQlName(field.name, "GraphQL ${request.kind.keyword()} '${request.name}' field")
        GraphQlFieldToGenerateVo(
          name = field.name,
          graphQlType = resolveLazyProviderGraphQlType(field, lazyProviderOverrides)
            ?: resolveFieldGraphQlType(request, field, referencedDtos, enums),
          nullable = resolveLazyProviderNullable(field, lazyProviderOverrides) ?: field.nullable,
          collection = field.collection,
          description = field.comment,
        )
      }
      .toMutableList()
    require(graphQlFields.isNotEmpty()) {
      "GraphQL ${request.kind.keyword()} '${request.name}' derived from DTO '${request.dto.name}' has no fields"
    }
    return BuiltType(
      type = GraphQlTypeDefinitionToGenerateVo(
        kind = request.kind,
        name = request.name,
        fields = graphQlFields,
        description = request.description,
      ),
      referencedDtos = referencedDtos,
      enums = enums,
    )
  }

  private fun buildLazyType(
    request: DeriveRequest,
    graphQlFields: List<GraphQlFieldToGenerateVo>,
  ): GraphQlLazyTypeToGenerateVo? {
    request.lazyProviders ?: return null
    val fields = metadata.fieldsOf(request.dto).orEmpty()
      .filterNot { it.name in request.exclude }
    val providerFields = fields.mapNotNull { it.dataProvider }
    require(providerFields.isNotEmpty()) {
      "GraphQL type '${request.name}' enables lazyProviders() but DTO '${request.dto.name}' has no provider fields"
    }
    val modelType = fields.mapNotNull { it.modelType }.distinct().singleOrNull()
      ?: throw IllegalArgumentException(
        "GraphQL type '${request.name}' lazyProviders() requires DTO '${request.dto.name}' to map from exactly one model type"
      )
    val sourceType = requireNotNull(request.sourceType) { "GraphQL type '${request.name}' lazyProviders() requires a source type" }
    val sourceAssemblerType = requireNotNull(request.sourceAssemblerType) {
      "GraphQL type '${request.name}' lazyProviders() requires a source assembler type"
    }
    val sourceFields = buildSourceFields(fields)
    val graphQlFieldsByName = graphQlFields.associateBy { it.name }
    return GraphQlLazyTypeToGenerateVo(
      graphQlTypeName = request.name,
      sourceType = sourceType,
      sourceAssemblerType = sourceAssemblerType,
      modelType = modelType,
      sourceFields = sourceFields.toMutableList(),
      providers = providerFields.map { provider ->
        provider.toLazyProvider(
          graphQlType = graphQlFieldsByName.getValue(provider.fieldName).graphQlType,
          nullable = graphQlFieldsByName.getValue(provider.fieldName).nullable,
        )
      }.toMutableList(),
    )
  }

  private fun buildSourceFields(fields: List<ResolvedDtoField>): List<GraphQlLazySourceFieldToGenerateVo> {
    val publicFields = fields
      .filter { it.dataProvider == null }
      .map { field ->
        GraphQlLazySourceFieldToGenerateVo(
          name = field.name,
          type = field.toKotlinFieldType(),
          modelFieldName = requireNotNull(field.modelFieldName),
          valueProvider = field.valueProvider,
          valueProviderParameterType = field.valueProviderParameterType.toGraphQlValueProviderParameterType(),
          valueProviderParameterField = field.valueProviderParameterField ?: requireNotNull(field.modelFieldName),
        )
      }
    val publicFieldNames = publicFields.mapTo(mutableSetOf()) { it.name }
    val keyFields = fields
      .mapNotNull { it.dataProvider }
      .filterNot { it.keySourceFieldName in publicFieldNames }
      .distinctBy { it.keySourceFieldName }
      .map { provider ->
        GraphQlLazySourceFieldToGenerateVo(
          name = provider.keySourceFieldName,
          type = provider.keySourceType.copy(nullable = provider.keySourceNullable),
          modelFieldName = provider.keySourceFieldName,
        )
      }
    return publicFields + keyFields
  }

  private fun ResolvedDtoField.toKotlinFieldType(): TypeName {
    val elementType = rawType.copy(nullable = !collection && nullable)
    return if (collection) {
      Collection::class.asClassName().parameterizedBy(rawType).copy(nullable = nullable)
    } else {
      elementType
    }
  }

  private fun ResolvedDtoProviderField.toLazyProvider(
    graphQlType: String,
    nullable: Boolean,
  ): GraphQlLazyProviderToGenerateVo =
    GraphQlLazyProviderToGenerateVo(
      fieldName = fieldName,
      graphQlType = graphQlType,
      nullable = nullable,
      providerType = providerType,
      keyType = keyType,
      valueType = valueType,
      keySourceFieldName = keySourceFieldName,
      keySourceType = keySourceType,
      keySourceNullable = keySourceNullable,
    )

  private fun resolveLazyProviderGraphQlType(
    field: ResolvedDtoField,
    overrides: Map<String, GraphQlLazyProviderOverride>,
  ): String? {
    val provider = field.dataProvider ?: return null
    return overrides[field.name]?.graphQlType ?: defaultProviderGraphQlType(provider.valueType)
  }

  private fun resolveLazyProviderNullable(
    field: ResolvedDtoField,
    overrides: Map<String, GraphQlLazyProviderOverride>,
  ): Boolean? {
    val provider = field.dataProvider ?: return null
    return overrides[field.name]?.nullable ?: provider.nullable
  }

  private fun defaultProviderGraphQlType(valueType: TypeName): String {
    val typeName = (valueType.copy(nullable = false) as? ClassName)?.simpleName ?: valueType.toString().substringAfterLast('.')
    return typeName.removeSuffix("Dto")
  }

  private fun ValueProviderParameterType.toGraphQlValueProviderParameterType(): GraphQlValueProviderParameterType =
    when (this) {
      ValueProviderParameterType.FIELD -> GraphQlValueProviderParameterType.FIELD
      ValueProviderParameterType.OBJECT -> GraphQlValueProviderParameterType.OBJECT
    }

  private fun resolveFieldGraphQlType(
    request: DeriveRequest,
    field: ResolvedDtoField,
    referencedDtos: MutableList<CodegenDto>,
    enums: MutableList<GraphQlTypeDefinitionToGenerateVo>,
  ): String {
    field.dtoRef?.let { refDto ->
      referencedDtos.add(refDto)
      return graphQlNameOf(refDto)
    }
    if (field.id) {
      return "ID"
    }
    typeMapper.scalarOf(field.rawType)?.let { return it }
    val enumClass = (field.rawType as? ClassName)?.let { loadClass(it.reflectionName()) }
    if (enumClass != null && enumClass.isEnum) {
      val enumName = enumClass.simpleName
      requireGraphQlName(enumName, "GraphQL enum")
      enums.add(buildEnum(enumClass, enumName))
      return enumName
    }
    throw IllegalArgumentException(
      "GraphQL derivation: cannot map field '${field.name}' of DTO '${request.dto.name}' " +
        "(type ${field.rawType}) to a GraphQL type. Register a scalar via mapScalar(...), " +
        "use fromRef/fieldRef for DTO references, or declare the type manually.",
    )
  }

  private fun buildEnum(enumClass: Class<*>, enumName: String): GraphQlTypeDefinitionToGenerateVo {
    val values = (enumClass.enumConstants ?: emptyArray())
      .filterIsInstance<Enum<*>>()
      .map { GraphQlEnumValueToGenerateVo(name = it.name) }
      .toMutableList()
    return GraphQlTypeDefinitionToGenerateVo(
      kind = GraphQlTypeDefinitionKind.ENUM,
      name = enumName,
      enumValues = values,
    )
  }

  private fun graphQlNameOf(dto: CodegenDto): String = dtoGraphQlNames.getOrPut(dto) { dto.name }

  private fun registerName(dto: CodegenDto, name: String) {
    val existing = dtoGraphQlNames[dto]
    require(existing == null || existing == name) {
      "GraphQL DTO '${dto.name}' is already derived as '$existing'; cannot also derive it as '$name'"
    }
    dtoGraphQlNames[dto] = name
  }

  private fun loadClass(reflectionName: String): Class<*>? =
    try {
      Class.forName(
        reflectionName,
        false,
        Thread.currentThread().contextClassLoader ?: javaClass.classLoader,
      )
    } catch (e: ClassNotFoundException) {
      null
    } catch (e: LinkageError) {
      null
    }

  private fun GraphQlTypeDefinitionKind.keyword(): String = when (this) {
    GraphQlTypeDefinitionKind.TYPE -> "type"
    GraphQlTypeDefinitionKind.INPUT -> "input"
    GraphQlTypeDefinitionKind.ENUM -> "enum"
    GraphQlTypeDefinitionKind.SCALAR -> "scalar"
    GraphQlTypeDefinitionKind.UNION -> "union"
  }
}
