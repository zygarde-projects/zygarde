package zygarde.codegen.dsl.graphql

import zygarde.codegen.dsl.meta.ResolvedDtoProviderField

class DslGraphQlDtoTypeDefinition internal constructor(
  private val graphQlTypeName: String,
  providerFields: List<ResolvedDtoProviderField>,
) {
  private val providersByName = providerFields.associateBy { it.fieldName }
  private var lazyProvidersConfig: GraphQlLazyProvidersConfig? = null

  fun lazyProviders(dsl: DslGraphQlLazyProviders.() -> Unit = {}) {
    val providers = DslGraphQlLazyProviders(graphQlTypeName, providersByName).also(dsl)
    lazyProvidersConfig = GraphQlLazyProvidersConfig(overrides = providers.build())
  }

  internal fun lazyProvidersConfig(): GraphQlLazyProvidersConfig? = lazyProvidersConfig
}
