package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import kotlin.reflect.KClass

class DslGraphQlTypeDefinition private constructor(
  private val kind: GraphQlTypeDefinitionKind,
  private val name: String,
) {
  private val fields: MutableList<GraphQlFieldToGenerateVo> = mutableListOf()
  private val enumValues: MutableList<String> = mutableListOf()

  fun field(name: String, graphQlType: String, nullable: Boolean = false, defaultValue: String? = null) {
    requireDefaultValueSupported(defaultValue)
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
    enumValues.add(name)
  }

  private fun requireDefaultValueSupported(defaultValue: String?) {
    require(defaultValue == null || kind == GraphQlTypeDefinitionKind.INPUT) {
      "GraphQL field default values are only supported on input fields"
    }
  }

  fun toGraphQlTypeDefinitionToGenerateVo(): GraphQlTypeDefinitionToGenerateVo {
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
