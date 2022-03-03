package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.asTypeName
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.data.api.PageDto
import kotlin.reflect.KClass

class DslApi(
  private val config: WebMvcDslCodegenConfig,
  private val apiName: String,
  private val basePath: String? = null,
) {

  var feignUrlProperty: String? = null
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

  @JvmName("getWithReified")
  inline fun <reified REQ, reified RES> get(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.GET, REQ::class, emptyList(), RES::class, emptyList(), dsl)
  }

  @JvmName("postWithReified")
  inline fun <reified REQ, reified RES> post(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.POST, REQ::class, emptyList(), RES::class, emptyList(), dsl)
  }

  @JvmName("putWithReified")
  inline fun <reified REQ, reified RES> put(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.PUT, REQ::class, emptyList(), RES::class, emptyList(), dsl)
  }

  @JvmName("getWithReifiedForList")
  inline fun <reified REQ, reified RES_IN_LIST> getList(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.GET, REQ::class, emptyList(), Collection::class, listOf(RES_IN_LIST::class), dsl)
  }

  @JvmName("postWithReifiedForList")
  inline fun <reified REQ, reified RES_IN_LIST> postList(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.POST, REQ::class, emptyList(), Collection::class, listOf(RES_IN_LIST::class), dsl)
  }

  @JvmName("getWithReifiedForPage")
  inline fun <reified REQ, reified RES_IN_LIST> getPage(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.GET, REQ::class, emptyList(), PageDto::class, listOf(RES_IN_LIST::class), dsl)
  }

  @JvmName("postWithReifiedForPage")
  inline fun <reified REQ, reified RES_IN_LIST> postPage(functionName: String, path: String, crossinline dsl: (DslApiFunction.() -> Unit)) {
    buildForMethodReified(functionName, path, RequestMethod.POST, REQ::class, emptyList(), PageDto::class, listOf(RES_IN_LIST::class), dsl)
  }

  inline fun buildForMethodReified(
    functionName: String,
    path: String,
    method: RequestMethod,
    reqClass: KClass<*>?,
    reqClassGenericClasses: List<KClass<*>>,
    resClass: KClass<*>?,
    resClassGenericClasses: List<KClass<*>>,
    crossinline dsl: DslApiFunction.() -> Unit
  ) {
    buildForMethod(functionName, path, method) {
      requestType = reqClass?.takeUnless { it == Unit::class }?.asTypeName()
      requestTypeGenericArguments = reqClassGenericClasses.map { it.asTypeName() }
      responseType = resClass?.takeUnless { it == Unit::class }?.asTypeName()
      responseTypeGenericArguments = resClassGenericClasses.map { it.asTypeName() }
      dsl.invoke(this)
    }
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
      feignUrlProperty = feignUrlProperty,
    )
  }

  fun buildForMethod(
    functionName: String,
    path: String,
    method: RequestMethod,
    dsl: DslApiFunction.() -> Unit
  ) {
    val dslApiFunction = DslApiFunction(functionName, path, method).also(dsl)
    functions.add(dslApiFunction.toApiFunctionToGenerate())
  }
}
