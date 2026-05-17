package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

/**
 * Maps resolved Kotlin DTO field types to GraphQL scalar type names while
 * deriving GraphQL types from model-mapping metadata.
 *
 * Built-in mappings mirror [defaultGraphQlType]; additional scalars (date/time,
 * BigDecimal, ...) can be registered per schema via [DslGraphQlSchema.mapScalar].
 */
class GraphQlTypeMapper {
  private val scalarByName: MutableMap<String, String> = mutableMapOf(
    "kotlin.String" to "String",
    "java.lang.String" to "String",
    "kotlin.Int" to "Int",
    "java.lang.Integer" to "Int",
    "kotlin.Long" to "Long",
    "java.lang.Long" to "Long",
    "kotlin.Boolean" to "Boolean",
    "java.lang.Boolean" to "Boolean",
    "kotlin.Float" to "Float",
    "java.lang.Float" to "Float",
    "kotlin.Double" to "Float",
    "java.lang.Double" to "Float",
  )

  /** Register (or override) the GraphQL scalar name used for a Kotlin type. */
  fun register(type: KClass<*>, graphQlType: String) {
    type.qualifiedName?.let { scalarByName[it] = graphQlType }
    scalarByName[type.java.name] = graphQlType
  }

  /** The GraphQL scalar name for [type], or null when [type] is not a known scalar. */
  fun scalarOf(type: TypeName): String? {
    val className = type as? ClassName ?: return null
    return scalarByName[className.canonicalName] ?: scalarByName[className.reflectionName()]
  }
}
