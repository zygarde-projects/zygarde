package zygarde.codegen.model

import com.squareup.kotlinpoet.TypeName
import org.springframework.web.bind.annotation.RequestMethod

data class ApiFunctionVo(
  var method: RequestMethod,
  var functionName: String,
  var description: String = "",
  var path: String,
  var pathVariables: Map<String, TypeName> = emptyMap(),
  var requestName: String = "req",
  var requestType: TypeName? = null,
  var requestTypeGenericArguments: List<TypeName> = emptyList(),
  var responseType: TypeName? = null,
  var responseTypeGenericArguments: List<TypeName> = emptyList(),
  var serviceName: String? = null,
  var serviceFunctionName: String? = null,
  var postProcessing: Boolean = false,
  var postProcessingExtraParameters: Map<String, TypeName> = emptyMap(),
  var authenticationDetailName: String = "authenticationDetail",
  var authenticationDetailType: TypeName? = null,
)
