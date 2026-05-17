package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import zygarde.codegen.dsl.extensions.asModelMetaField
import zygarde.codegen.model.graphql.GraphQlEnumValueToGenerateVo
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class DslGraphQlEntityProjectionDefinition internal constructor(
  kind: GraphQlTypeDefinitionKind,
  name: String,
  private val typeMapper: GraphQlTypeMapper,
  private val referenceTypeResolver: (KClass<*>, String) -> String,
) {
  private val typeDefinitionName = name
  private val typeDefinition: DslGraphQlTypeDefinition = when (kind) {
    GraphQlTypeDefinitionKind.TYPE -> DslGraphQlTypeDefinition.type(name)
    GraphQlTypeDefinitionKind.INPUT -> DslGraphQlTypeDefinition.input(name)
    GraphQlTypeDefinitionKind.ENUM,
    GraphQlTypeDefinitionKind.SCALAR,
    GraphQlTypeDefinitionKind.UNION -> error("GraphQL entity projection supports only type and input definitions")
  }
  private val enumDefinitions: MutableList<GraphQlTypeDefinitionToGenerateVo> = mutableListOf()

  var description: String?
    get() = typeDefinition.description
    set(value) {
      typeDefinition.description = value
    }

  fun from(vararg props: KProperty1<*, *>) {
    props.forEach { addProperty(it, id = false) }
  }

  fun applyTo(vararg props: KProperty1<*, *>) {
    from(*props)
  }

  fun fromAutoIntId(vararg props: KProperty1<*, Int?>) {
    props.forEach { addProperty(it, id = true) }
  }

  fun fromAutoLongId(vararg props: KProperty1<*, Long?>) {
    props.forEach { addProperty(it, id = true) }
  }

  fun ref(
    name: String,
    graphQlType: String,
    nullable: Boolean = false,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    typeDefinition.field(
      name = name,
      graphQlType = graphQlType,
      nullable = nullable,
      description = description,
      deprecationReason = deprecationReason,
    )
  }

  inline fun <reified T : Any> ref(
    name: String,
    nullable: Boolean = false,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    ref(
      name = name,
      graphQlType = graphQlTypeOf(T::class, name),
      nullable = nullable,
      description = description,
      deprecationReason = deprecationReason,
    )
  }

  fun refCollection(
    name: String,
    graphQlType: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    typeDefinition.collectionField(
      name = name,
      graphQlType = graphQlType,
      nullable = nullable,
      itemNullable = itemNullable,
      description = description,
      deprecationReason = deprecationReason,
    )
  }

  inline fun <reified T : Any> refCollection(
    name: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    refCollection(
      name = name,
      graphQlType = graphQlTypeOf(T::class, name),
      nullable = nullable,
      itemNullable = itemNullable,
      description = description,
      deprecationReason = deprecationReason,
    )
  }

  internal fun build(): GraphQlEntityProjectionGenerateResult {
    return GraphQlEntityProjectionGenerateResult(
      typeDefinition = typeDefinition.toGraphQlTypeDefinitionToGenerateVo(),
      additionalTypeDefinitions = enumDefinitions,
    )
  }

  @PublishedApi
  internal fun graphQlTypeOf(type: KClass<*>, fieldName: String): String {
    return referenceTypeResolver(type, fieldName)
  }

  private fun addProperty(prop: KProperty1<*, *>, id: Boolean) {
    val field = prop.asModelMetaField()
    val resolvedType = resolveGraphQlType(field.fieldName, field.fieldClass, id)
    val description = field.comment.takeUnless { it.isBlank() }
    if (resolvedType.collection) {
      typeDefinition.collectionField(
        name = field.fieldName,
        graphQlType = resolvedType.graphQlType,
        nullable = field.fieldNullable,
        itemNullable = resolvedType.itemNullable,
        description = description,
      )
    } else {
      typeDefinition.field(
        name = field.fieldName,
        graphQlType = resolvedType.graphQlType,
        nullable = if (id) false else field.fieldNullable,
        description = description,
      )
    }
  }

  private fun resolveGraphQlType(fieldName: String, type: TypeName, id: Boolean): ResolvedGraphQlProjectionType {
    val collection = type.collectionElementOrNull()
    val rawType = (collection?.elementType ?: type).copy(nullable = false)
    if (id) {
      return ResolvedGraphQlProjectionType("ID", collection != null, collection?.itemNullable ?: false)
    }
    typeMapper.scalarOf(rawType)?.let {
      return ResolvedGraphQlProjectionType(it, collection != null, collection?.itemNullable ?: false)
    }
    val enumClass = (rawType as? ClassName)?.let { loadClass(it.reflectionName()) }
    if (enumClass != null && enumClass.isEnum) {
      val enumName = enumClass.simpleName
      requireGraphQlName(enumName, "GraphQL enum")
      enumDefinitions.add(buildEnum(enumClass, enumName))
      return ResolvedGraphQlProjectionType(enumName, collection != null, collection?.itemNullable ?: false)
    }
    throw IllegalArgumentException(
      "GraphQL entity projection '$typeDefinitionName': cannot map property '$fieldName' (type $type) to a GraphQL type. " +
        "Register a scalar via mapScalar(...), declare an enum, or use the manual type/input DSL.",
    )
  }

  private fun TypeName.collectionElementOrNull(): CollectionElement? {
    if (this is ParameterizedTypeName && rawType in collectionRawTypes) {
      val elementType = typeArguments.firstOrNull() ?: return null
      return CollectionElement(elementType.copy(nullable = false), elementType.isNullable)
    }
    return null
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

  private data class CollectionElement(
    val elementType: TypeName,
    val itemNullable: Boolean,
  )

  private data class ResolvedGraphQlProjectionType(
    val graphQlType: String,
    val collection: Boolean,
    val itemNullable: Boolean,
  )

  companion object {
    private val collectionRawTypes = setOf(
      Collection::class.asClassName(),
      List::class.asClassName(),
      Set::class.asClassName(),
      Iterable::class.asClassName(),
    )
  }
}

internal data class GraphQlEntityProjectionGenerateResult(
  val typeDefinition: GraphQlTypeDefinitionToGenerateVo,
  val additionalTypeDefinitions: List<GraphQlTypeDefinitionToGenerateVo>,
)
