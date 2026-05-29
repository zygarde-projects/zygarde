package zygarde.codegen.dsl.graphql

import zygarde.codegen.dsl.meta.ResolvedDtoProviderField
import zygarde.codegen.model.graphql.requireGraphQlName

internal data class GraphQlLazyProviderOverride(
  val graphQlType: String?,
  val nullable: Boolean?,
)

class DslGraphQlLazyProvider(
  private val fieldName: String,
) {
  private var graphQlType: String? = null
  private var nullable: Boolean? = null

  fun graphQlType(graphQlType: String) {
    requireGraphQlName(graphQlType, "GraphQL lazy provider field '$fieldName' type")
    this.graphQlType = graphQlType
  }

  fun nullable() {
    nullable = true
  }

  fun nonNull() {
    nullable = false
  }

  internal fun build(): GraphQlLazyProviderOverride =
    GraphQlLazyProviderOverride(graphQlType = graphQlType, nullable = nullable)
}

class DslGraphQlLazyProviders internal constructor(
  private val graphQlTypeName: String,
  private val providersByName: Map<String, ResolvedDtoProviderField>,
) {
  private val overrides = linkedMapOf<String, GraphQlLazyProviderOverride>()

  fun provider(fieldName: String, dsl: DslGraphQlLazyProvider.() -> Unit) {
    require(fieldName in providersByName) {
      "GraphQL type '$graphQlTypeName' cannot configure lazy provider '$fieldName' because the DTO metadata has no provider field '$fieldName'"
    }
    overrides[fieldName] = DslGraphQlLazyProvider(fieldName).also(dsl).build()
  }

  internal fun build(): Map<String, GraphQlLazyProviderOverride> = overrides
}

internal data class GraphQlLazyProvidersConfig(
  val overrides: Map<String, GraphQlLazyProviderOverride>,
)
