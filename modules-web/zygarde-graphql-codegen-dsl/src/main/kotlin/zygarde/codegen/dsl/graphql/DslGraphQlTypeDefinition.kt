package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlName
import zygarde.codegen.model.graphql.requireUniqueGraphQlName
import kotlin.reflect.KClass

class DslGraphQlTypeDefinition private constructor(
  private val kind: GraphQlTypeDefinitionKind,
  private val name: String,
) {
  private val fields: MutableList<GraphQlFieldToGenerateVo> = mutableListOf()
  private val enumValues: MutableList<String> = mutableListOf()

  init {
    requireGraphQlName(name, "GraphQL type definition name")
  }

  fun field(name: String, graphQlType: String, nullable: Boolean = false, defaultValue: String? = null) {
    requireDefaultValueSupported(defaultValue)
    requireGraphQlName(name, "GraphQL field name")
    requireGraphQlName(graphQlType, "GraphQL field type")
    requireUniqueGraphQlName(name, fields.map { it.name }, "GraphQL field")
    fields.add(
      GraphQlFieldToGenerateVo(
        name = name,
        graphQlType = graphQlType,
        nullable = nullable,
        defaultValue = defaultValue,
      )
    )
  }

  fun field(name: String, type: KClass<*>, nullable: Boolean = false, defaultValue: String? = null) {
    field(name, type.defaultGraphQlType(), nullable, defaultValue)
  }

  inline fun <reified T : Any> field(name: String, nullable: Boolean = false, defaultValue: String? = null) {
    field(name, T::class, nullable, defaultValue)
  }

  fun collectionField(
    name: String,
    graphQlType: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    requireDefaultValueSupported(defaultValue)
    requireGraphQlName(name, "GraphQL field name")
    requireGraphQlName(graphQlType, "GraphQL field type")
    requireUniqueGraphQlName(name, fields.map { it.name }, "GraphQL field")
    fields.add(
      GraphQlFieldToGenerateVo(
        name = name,
        graphQlType = graphQlType,
        nullable = nullable,
        collection = true,
        itemNullable = itemNullable,
        defaultValue = defaultValue,
      )
    )
  }

  fun collectionField(
    name: String,
    type: KClass<*>,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    collectionField(name, type.defaultGraphQlType(), nullable, itemNullable, defaultValue)
  }

  inline fun <reified T : Any> collectionField(
    name: String,
    nullable: Boolean = false,
    itemNullable: Boolean = false,
    defaultValue: String? = null,
  ) {
    collectionField(name, T::class, nullable, itemNullable, defaultValue)
  }

  fun value(name: String) {
    requireGraphQlName(name, "GraphQL enum value")
    requireUniqueGraphQlName(name, enumValues, "GraphQL enum value")
    enumValues.add(name)
  }

  inline fun <reified T : Enum<T>> values() {
    enumValues<T>().forEach { value(it.name) }
  }

  private fun requireDefaultValueSupported(defaultValue: String?) {
    require(defaultValue == null || kind == GraphQlTypeDefinitionKind.INPUT) {
      "GraphQL field default values are only supported on input fields"
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
    return GraphQlTypeDefinitionToGenerateVo(
      kind = kind,
      name = name,
      fields = fields,
      enumValues = enumValues,
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
