package zygarde.codegen.dsl.graphql

import zygarde.codegen.dsl.meta.ModelMappingMetadata
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo

abstract class GraphQlDslCodegen {
  private val config: GraphQlDslCodegenConfig = GraphQlDslCodegenConfig(
    controllerPackage = System.getProperty("zygarde.codegen.dsl.graphql.controller.package") ?: "zygarde.generated.graphql",
    serviceInterfacePackage = System.getProperty("zygarde.codegen.dsl.graphql.service-interface.package") ?: "zygarde.generated.graphql.service",
  )
  val apisToGenerate: MutableList<GraphQlApiToGenerateVo> = mutableListOf()

  /**
   * Model-mapping DTO metadata that `typeFrom` / `inputFrom` derive GraphQL types
   * from. [main] assigns this from the model-mapping specs found on the codegen
   * classpath before invoking [codegen]; tests may set it directly.
   */
  var modelMappingMetadata: ModelMappingMetadata = ModelMappingMetadata.EMPTY

  abstract fun codegen()

  protected fun schema(schemaName: String, dsl: DslGraphQlSchema.() -> Unit) {
    val dslSchema = DslGraphQlSchema(config, schemaName, modelMappingMetadata).also(dsl)
    apisToGenerate.add(dslSchema.toGraphQlApiToGenerateVo())
  }
}
