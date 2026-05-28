package zygarde.codegen.dsl.sqlapi

abstract class SqlApiDslCodegen {
  private val config: SqlApiDslCodegenConfig = SqlApiDslCodegenConfig(
    dtoPackage = System.getProperty("zygarde.codegen.dsl.sql-api.dto.package") ?: "zygarde.generated.dto",
    apiInterfacePackage = System.getProperty("zygarde.codegen.dsl.sql-api.api-interface.package") ?: "zygarde.generated.api",
    controllerPackage = System.getProperty("zygarde.codegen.dsl.sql-api.controller.package") ?: "zygarde.generated.controller",
    serviceInterfacePackage = System.getProperty("zygarde.codegen.dsl.sql-api.service-interface.package") ?: "zygarde.generated.service",
    serviceImplPackage = System.getProperty("zygarde.codegen.dsl.sql-api.service-impl.package") ?: "zygarde.generated.service.impl",
  )

  val apisToGenerate: MutableList<SqlApiToGenerateVo> = mutableListOf()

  abstract fun codegen()

  protected fun sqlApi(apiName: String, basePath: String, dsl: DslSqlApi.() -> Unit) {
    val dslApi = DslSqlApi(config, apiName, basePath).also(dsl)
    apisToGenerate.add(dslApi.toSqlApiToGenerateVo())
  }
}
