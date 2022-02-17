package zygarde.codegen.dsl.webmvc

import zygarde.codegen.model.ApiToGenerateVo

abstract class WebMvcDslCodegen {

  private val config: WebMvcDslCodegenConfig = WebMvcDslCodegenConfig(
    apiInterfacePackage = System.getProperty("zygarde.codegen.dsl.webmvc.api-interface.package") ?: "zygarde.generated.api",
    controllerPackage = System.getProperty("zygarde.codegen.dsl.webmvc.controller.package") ?: "zygarde.generated.api.impl",
    serviceInterfacePackage = System.getProperty("zygarde.codegen.dsl.webmvc.service-interface.package") ?: "zygarde.generated.service"
  )
  val apisToGenerate: MutableList<ApiToGenerateVo> = mutableListOf()

  abstract fun codegen()

  protected fun api(apiName: String, dsl: (DslApi.() -> Unit)) {
    api(apiName, null, dsl)
  }

  protected fun api(apiName: String, basePath: String?, dsl: (DslApi.() -> Unit)) {
    val dslApi = DslApi(config, apiName, basePath).also(dsl)
    apisToGenerate.add(dslApi.toApiToGenerateVo())
  }
}
