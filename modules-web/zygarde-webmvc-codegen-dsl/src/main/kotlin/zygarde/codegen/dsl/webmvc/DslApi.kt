package zygarde.codegen.dsl.webmvc

import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo

class DslApi(
  private val config: WebMvcDslCodegenConfig,
  private val apiName: String,
  private val basePath: String? = null,
) {

  private val functions: MutableList<ApiFunctionToGenerateVo> = mutableListOf()

  fun get(functionName: String, path: String, dsl: (DslApiFunction.() -> Unit)) {
    buildForMethod(functionName, path, RequestMethod.GET, dsl)
  }

  fun put(functionName: String, path: String, dsl: (DslApiFunction.() -> Unit)) {
    buildForMethod(functionName, path, RequestMethod.PUT, dsl)
  }

  fun post(functionName: String, path: String, dsl: (DslApiFunction.() -> Unit)) {
    buildForMethod(functionName, path, RequestMethod.POST, dsl)
  }

  fun delete(functionName: String, path: String, dsl: (DslApiFunction.() -> Unit)) {
    buildForMethod(functionName, path, RequestMethod.DELETE, dsl)
  }

  fun toApiToGenerateVo(): ApiToGenerateVo {
    return ApiToGenerateVo(
      apiInterfacePackage = config.apiInterfacePackage,
      controllerPackage = config.controllerPackage,
      serviceInterfacePackage = config.serviceInterfacePackage,
      apiName = apiName,
      basePath = basePath,
      functions = functions,
      separateFeign = true,
    )
  }

  private fun buildForMethod(
    functionName: String,
    path: String,
    method: RequestMethod,
    dsl: DslApiFunction.() -> Unit
  ) {
    val dslApiFunction = DslApiFunction(functionName, path, method).also(dsl)
    functions.add(dslApiFunction.toApiFunctionToGenerate())
  }
}
