package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.data.api.PageDto
import kotlin.reflect.KClass

class DslApiFunction(
  val functionName: String,
  val path: String,
  val method: RequestMethod,
) {

  var description: String = ""
  var requestName: String = "req"
  var serviceName: String? = null
  var serviceFunctionName: String? = null
  var requestType: TypeName? = null
  var requestTypeGenericArguments: List<TypeName> = emptyList()
  var responseType: TypeName? = null
  var responseTypeGenericArguments: List<TypeName> = emptyList()
  var servicePostProcessing: Boolean = false
  var servicePostProcessingExtraParamType: TypeName? = null
  var authenticationDetailName: String = "auth"
  var authenticationDetailType: TypeName? = null

  private val pathVariables: MutableMap<String, KClass<*>> = mutableMapOf()

  fun pathVariable(name: String, type: KClass<*>) {
    pathVariables(name to type)
  }

  inline fun <reified T : Any> pathVariable(name: String) {
    pathVariables(name to T::class)
  }

  fun pathVariables(vararg pairs: Pair<String, KClass<*>>) {
    pathVariables.putAll(pairs)
  }

  inline fun <reified T : Any> req(name: String = "req") {
    requestName = name
    requestType = T::class.asTypeName()
  }

  inline fun <reified T : Any> reqCollection(name: String = "reqList") {
    requestName = name
    requestType = Collection::class.asTypeName()
    requestTypeGenericArguments = listOf(T::class.asTypeName())
  }

  inline fun <reified T : Any> res() {
    responseType = T::class.asTypeName()
  }

  inline fun <reified T : Any> resCollection() {
    responseType = Collection::class.asTypeName()
    responseTypeGenericArguments = listOf(T::class.asTypeName())
  }

  inline fun <reified T : Any> resPage() {
    responseType = PageDto::class.asTypeName()
    responseTypeGenericArguments = listOf(T::class.asTypeName())
  }

  inline fun <reified T : Any> auth(name: String = "auth") {
    authenticationDetailName = name
    authenticationDetailType = T::class.asTypeName()
  }

  inline fun <reified T : Any> servicePostProcessing() {
    servicePostProcessing = true
    servicePostProcessingExtraParamType = T::class.asTypeName()
  }

  fun toApiFunctionToGenerate(): ApiFunctionToGenerateVo {
    return ApiFunctionToGenerateVo(
      method = method,
      functionName = functionName,
      description = description,
      path = path,
      pathVariables = pathVariables.mapValues { e -> e.value.asTypeName() },
      requestName = requestName,
      requestType = requestType,
      requestTypeGenericArguments = requestTypeGenericArguments,
      responseType = responseType,
      responseTypeGenericArguments = responseTypeGenericArguments,
      serviceName = serviceName,
      serviceFunctionName = serviceFunctionName ?: functionName,
      postProcessing = servicePostProcessingExtraParamType != null || servicePostProcessing,
      postProcessingParamType = servicePostProcessingExtraParamType,
      authenticationDetailName = authenticationDetailName,
      authenticationDetailType = authenticationDetailType
    )
  }
}
