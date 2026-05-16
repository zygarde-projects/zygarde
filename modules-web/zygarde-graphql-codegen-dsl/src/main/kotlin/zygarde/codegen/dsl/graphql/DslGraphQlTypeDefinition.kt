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

  fun field(name: String, graphQlType: String, nullable: Boolean = false) {
    fields.add(
      GraphQlFieldToGenerateVo(
        name = name,
        graphQlType = graphQlType,
        nullable = nullable,
      )
    )
  }

  fun field(name: String, type: KClass<*>, nullable: Boolean = false) {
    field(name, type.defaultGraphQlType(), nullable)
  }

  inline fun <reified T : Any> field(name: String, nullable: Boolean = false) {
    field(name, T::class, nullable)
  }

  fun collectionField(name: String, graphQlType: String, nullable: Boolean = false, itemNullable: Boolean = false) {
    fields.add(
      GraphQlFieldToGenerateVo(
        name = name,
        graphQlType = graphQlType,
        nullable = nullable,
        collection = true,
        itemNullable = itemNullable,
      )
    )
  }

  fun toGraphQlTypeDefinitionToGenerateVo(): GraphQlTypeDefinitionToGenerateVo {
    return GraphQlTypeDefinitionToGenerateVo(
      kind = kind,
      name = name,
      fields = fields,
    )
  }

  companion object {
    fun type(name: String): DslGraphQlTypeDefinition {
      return DslGraphQlTypeDefinition(GraphQlTypeDefinitionKind.TYPE, name)
    }

    fun input(name: String): DslGraphQlTypeDefinition {
      return DslGraphQlTypeDefinition(GraphQlTypeDefinitionKind.INPUT, name)
    }
  }
}
