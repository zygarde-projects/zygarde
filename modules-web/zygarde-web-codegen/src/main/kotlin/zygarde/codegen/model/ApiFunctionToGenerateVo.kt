package zygarde.codegen.model

import com.squareup.kotlinpoet.TypeName
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.RequestBodyContentType

data class ApiFunctionToGenerateVo(
  var method: RequestMethod,
  var functionName: String,
  var description: String = "",
  var path: String,
  var pathVariables: Map<String, TypeName> = emptyMap(),
  var requestParams: Map<String, TypeName> = emptyMap(),
  var requestName: String = "req",
  var requestType: TypeName? = null,
  var requestTypeGenericArguments: List<TypeName> = emptyList(),
  var requestBodyContentType: RequestBodyContentType = RequestBodyContentType.DEFAULT,
  var responseType: TypeName? = null,
  var responseTypeGenericArguments: List<TypeName> = emptyList(),
  var serviceName: String? = null,
  var serviceFunctionName: String? = null,
  var postProcessing: Boolean = false,
  var postProcessingParamType: TypeName? = null,
  var responseStatus: HttpStatus? = null,
  var authenticationDetailName: String = "auth",
  var authenticationDetailType: TypeName? = null,
  var deprecated: Deprecated? = null,
)
