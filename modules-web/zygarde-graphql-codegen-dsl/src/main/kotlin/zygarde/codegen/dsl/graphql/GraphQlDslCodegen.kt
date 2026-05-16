package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo

abstract class GraphQlDslCodegen {
  private val config: GraphQlDslCodegenConfig = GraphQlDslCodegenConfig(
    controllerPackage = System.getProperty("zygarde.codegen.dsl.graphql.controller.package") ?: "zygarde.generated.graphql",
    serviceInterfacePackage = System.getProperty("zygarde.codegen.dsl.graphql.service-interface.package") ?: "zygarde.generated.graphql.service",
  )
  val apisToGenerate: MutableList<GraphQlApiToGenerateVo> = mutableListOf()

  abstract fun codegen()

  protected fun schema(schemaName: String, dsl: DslGraphQlSchema.() -> Unit) {
    val dslSchema = DslGraphQlSchema(config, schemaName).also(dsl)
    apisToGenerate.add(dslSchema.toGraphQlApiToGenerateVo())
  }
}
