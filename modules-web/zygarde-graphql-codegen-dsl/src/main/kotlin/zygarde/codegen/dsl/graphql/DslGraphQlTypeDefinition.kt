package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.GraphQlEnumValueToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlDeprecationReason
import zygarde.codegen.model.graphql.requireGraphQlDescription
import zygarde.codegen.model.graphql.requireGraphQlName
import zygarde.codegen.model.graphql.requireUniqueGraphQlName
import kotlin.reflect.KClass

class DslGraphQlTypeDefinition private constructor(
  private val kind: GraphQlTypeDefinitionKind,
  private val name: String,
) {
  var description: String? = null
  private val fields: MutableList<GraphQlFieldToGenerateVo> = mutableListOf()
  private val enumValues: MutableList<GraphQlEnumValueToGenerateVo> = mutableListOf()

  init {
    requireGraphQlName(name, "GraphQL type definition name")
  }

  fun field(
    name: String,
    graphQlType: String,
    nullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    requireDefaultValueSupported(defaultValue)
    requireGraphQlName(name, "GraphQL field name")
    requireGraphQlName(graphQlType, "GraphQL field type")
    requireUniqueGraphQlName(name, fields.map { it.name }, "GraphQL field")
    requireGraphQlDescription(description, "GraphQL ${kind.schemaKeyword()} '${this.name}' field '$name'")
    requireFieldDeprecation(name, nullable, defaultValue, deprecationReason)
    fields.add(
      GraphQlFieldToGenerateVo(
        name = name,
        graphQlType = graphQlType,
        nullable = nullable,
        defaultValue = defaultValue,
        description = description,
        deprecationReason = deprecationReason,
      )
    )
  }

  fun field(
    name: String,
    type: KClass<*>,
    nullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    field(name, type.defaultGraphQlType(), nullable, defaultValue, description, deprecationReason)
  }

  inline fun <reified T : Any> field(
    name: String,
    nullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    field(name, T::class, nullable, defaultValue, description, deprecationReason)
  }

  fun collectionField(
    name: String,
    graphQlType: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    requireDefaultValueSupported(defaultValue)
    requireGraphQlName(name, "GraphQL field name")
    requireGraphQlName(graphQlType, "GraphQL field type")
    requireUniqueGraphQlName(name, fields.map { it.name }, "GraphQL field")
    requireGraphQlDescription(description, "GraphQL ${kind.schemaKeyword()} '${this.name}' field '$name'")
    requireFieldDeprecation(name, nullable, defaultValue, deprecationReason)
    fields.add(
      GraphQlFieldToGenerateVo(
        name = name,
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

  fun collectionField(
    name: String,
    type: KClass<*>,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    collectionField(name, type.defaultGraphQlType(), nullable, itemNullable, defaultValue, description, deprecationReason)
  }

  inline fun <reified T : Any> collectionField(
    name: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
    description: String? = null,
    deprecationReason: String? = null,
  ) {
    collectionField(name, T::class, nullable, itemNullable, defaultValue, description, deprecationReason)
  }

  fun value(name: String, description: String? = null, deprecationReason: String? = null) {
    requireGraphQlName(name, "GraphQL enum value")
    requireUniqueGraphQlName(name, enumValues.map { it.name }, "GraphQL enum value")
    val valueLabel = "GraphQL ${kind.schemaKeyword()} '${this.name}' value '$name'"
    requireGraphQlDescription(description, valueLabel)
    requireGraphQlDeprecationReason(deprecationReason, valueLabel)
    enumValues.add(
      GraphQlEnumValueToGenerateVo(name = name, description = description, deprecationReason = deprecationReason)
    )
  }

  inline fun <reified T : Enum<T>> values() {
    enumValues<T>().forEach { value(it.name) }
  }

  private fun requireDefaultValueSupported(defaultValue: String?) {
    require(defaultValue == null || kind == GraphQlTypeDefinitionKind.INPUT) {
      "GraphQL field default values are only supported on input fields"
    }
  }

  private fun requireFieldDeprecation(
    name: String,
    nullable: Boolean,
    defaultValue: String?,
    deprecationReason: String?,
  ) {
    requireGraphQlDeprecationReason(deprecationReason, "GraphQL ${kind.schemaKeyword()} '${this.name}' field '$name'")
    require(
      deprecationReason == null ||
        kind != GraphQlTypeDefinitionKind.INPUT ||
        nullable ||
        defaultValue != null
    ) {
      "GraphQL input '${this.name}' field '$name' cannot be deprecated because it is a required input field"
    }
  }

  fun toGraphQlTypeDefinitionToGenerateVo(): GraphQlTypeDefinitionToGenerateVo {
    when (kind) {
      GraphQlTypeDefinitionKind.TYPE,
      GraphQlTypeDefinitionKind.INPUT -> {
        require(fields.isNotEmpty()) {
          "GraphQL ${kind.schemaKeyword()} '$name' must declare at least one field"
        }
      }
      GraphQlTypeDefinitionKind.ENUM -> {
        require(enumValues.isNotEmpty()) {
          "GraphQL enum '$name' must declare at least one value"
        }
      }
      GraphQlTypeDefinitionKind.SCALAR -> Unit
    }
    requireGraphQlDescription(description, "GraphQL ${kind.schemaKeyword()} '$name'")
    return GraphQlTypeDefinitionToGenerateVo(
      kind = kind,
      name = name,
      fields = fields,
      enumValues = enumValues,
      description = description,
    )
  }

  companion object {
    fun type(name: String): DslGraphQlTypeDefinition {
      return DslGraphQlTypeDefinition(GraphQlTypeDefinitionKind.TYPE, name)
    }

    fun input(name: String): DslGraphQlTypeDefinition {
      return DslGraphQlTypeDefinition(GraphQlTypeDefinitionKind.INPUT, name)
    }

    fun enumType(name: String): DslGraphQlTypeDefinition {
      return DslGraphQlTypeDefinition(GraphQlTypeDefinitionKind.ENUM, name)
    }
  }
}

private fun GraphQlTypeDefinitionKind.schemaKeyword(): String {
  return when (this) {
    GraphQlTypeDefinitionKind.TYPE -> "type"
    GraphQlTypeDefinitionKind.INPUT -> "input"
    GraphQlTypeDefinitionKind.ENUM -> "enum"
    GraphQlTypeDefinitionKind.SCALAR -> "scalar"
  }
}
