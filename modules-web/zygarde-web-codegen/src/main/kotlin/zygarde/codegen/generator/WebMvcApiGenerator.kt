package zygarde.codegen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.codegen.model.WebApiGenerateResult
import java.util.LinkedList
import javax.validation.Valid

class WebMvcApiGenerator(
  private val apis: Collection<ApiToGenerateVo>
) {
  private val apiInterfaceFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val apiInterfaceBuilderMap = mutableMapOf<String, TypeSpec.Builder>()
  private val feignApiInterfaceFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val feignApiInterfaceBuilderMap = mutableMapOf<String, TypeSpec.Builder>()
  private val webMcvControllerFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val webMcvControllerBuilderMap = mutableMapOf<String, TypeSpec.Builder>()
  private val serviceInterfaceFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val serviceInterfaceBuilderMap = mutableMapOf<String, TypeSpec.Builder>()

  private val beanFunc = MemberName("zygarde.core.di.DiServiceContext", "bean")

  fun generateApis(): WebApiGenerateResult {
    apis.forEach { it.generate() }

    apiInterfaceFileSpecBuilderMap.forEach { (apiName, fileSpecBuilder) ->
      apiInterfaceBuilderMap[apiName]?.build()?.let(fileSpecBuilder::addType)
    }
    feignApiInterfaceFileSpecBuilderMap.forEach { (apiName, fileSpecBuilder) ->
      feignApiInterfaceBuilderMap[apiName]?.build()?.let(fileSpecBuilder::addType)
    }
    webMcvControllerFileSpecBuilderMap.forEach { (apiName, fileSpecBuilder) ->
      webMcvControllerBuilderMap[apiName]?.build()?.let(fileSpecBuilder::addType)
    }
    serviceInterfaceFileSpecBuilderMap.forEach { (serviceName, fileSpecBuilder) ->
      serviceInterfaceBuilderMap[serviceName]?.build()?.let(fileSpecBuilder::addType)
    }
    return WebApiGenerateResult(
      apiInterfaces = apiInterfaceFileSpecBuilderMap.values.map { it.build() },
      feignApiInterfaces = feignApiInterfaceFileSpecBuilderMap.values.map { it.build() },
      controllers = webMcvControllerFileSpecBuilderMap.values.map { it.build() },
      serviceInterfaces = serviceInterfaceFileSpecBuilderMap.values.map { it.build() },
    )
  }

  private fun ApiToGenerateVo.generate() {
    val apiPackage = apiInterfacePackage

    apiInterfaceFileSpecBuilderMap.getOrPut(apiName) {
      FileSpec.builder(apiPackage, apiName)
    }

    val feignClientAnnotation = AnnotationSpec.builder(FeignClient::class)
      .addMember("name=%S", apiName)
      .build()

    val apiInterfaceBuilder = apiInterfaceBuilderMap.getOrPut(apiName) {
      TypeSpec.interfaceBuilder(apiName)
    }
    val feignApiName = "${apiName}Feign"

    if (separateFeign) {
      feignApiInterfaceFileSpecBuilderMap.getOrPut(feignApiName) {
        FileSpec.builder(apiPackage, feignApiName)
      }
    } else {
      apiInterfaceBuilder.addAnnotation(feignClientAnnotation)
    }

    val feignApiInterfaceBuilder = feignApiInterfaceBuilderMap.getOrPut(feignApiName) {
      TypeSpec.interfaceBuilder(feignApiName)
        .addSuperinterface(ClassName(apiPackage, apiName))
        .addAnnotation(feignClientAnnotation)
    }

    val apiImplName = "${apiName}Controller"
    webMcvControllerFileSpecBuilderMap.getOrPut(apiImplName) {
      FileSpec.builder(controllerPackage, apiImplName)
    }
    val webMvcControllerBuilder = webMcvControllerBuilderMap.getOrPut(apiImplName) {
      TypeSpec.classBuilder(apiImplName)
        .addAnnotation(RestController::class)
        .addSuperinterface(ClassName(apiPackage, apiName))
        .addAnnotation(
          AnnotationSpec.builder(Tag::class)
            .addMember("name=%S", apiName)
            .build()
        )
    }

    functions.forEach { func ->
      val serviceInterfaceName = func.serviceName ?: (apiName + "Service")
      serviceInterfaceFileSpecBuilderMap.getOrPut(serviceInterfaceName) {
        FileSpec.builder(serviceInterfacePackage, serviceInterfaceName)
      }
      val serviceInterfaceBuilder = serviceInterfaceBuilderMap.getOrPut(serviceInterfaceName) {
        TypeSpec.interfaceBuilder(serviceInterfaceName)
      }
      val apiFunctionName = func.functionName
      val serviceFunctionName = func.serviceFunctionName ?: func.functionName

      val methodAnnotation = AnnotationSpec
        .builder(
          when (func.method) {
            RequestMethod.POST -> PostMapping::class
            RequestMethod.PUT -> PutMapping::class
            RequestMethod.DELETE -> DeleteMapping::class
            else -> GetMapping::class
          }
        )
        .also { annSpec ->
          if (func.path.isNotEmpty()) {
            annSpec.addMember("value=[%S]", basePath.orEmpty() + func.path)
          }
        }
        .build()

      val apiFuncBuilder = FunSpec.builder(apiFunctionName)
        .addModifiers(KModifier.ABSTRACT)
      if (!separateFeign) {
        apiFuncBuilder.addAnnotation(methodAnnotation)
      }

      val feignApiFuncBuilder = FunSpec.builder(apiFunctionName)
        .addModifiers(KModifier.ABSTRACT, KModifier.OVERRIDE)
        .addAnnotation(methodAnnotation)

      val webMvcFuncBuilder = FunSpec.builder(apiFunctionName)
        .addModifiers(KModifier.OVERRIDE)
        .addAnnotation(methodAnnotation)
        .addAnnotation(
          AnnotationSpec.builder(Operation::class)
            .addMember("summary=%S", listOf(apiFunctionName, func.description).filter { it.isNotEmpty() }.joinToString(" "))
            .build()
        )

      val servicePostProcessingFuncBuilder = if (func.postProcessing) {
        FunSpec.builder(serviceFunctionName + "PostProcessing")
          .addModifiers(KModifier.ABSTRACT)
      } else {
        null
      }

      val serviceFuncBuilder = FunSpec.builder(serviceFunctionName)
        .addModifiers(KModifier.ABSTRACT)

      val paramsToCallServiceInterface = mutableListOf<String>()

      func.pathVariables.forEach { (pathVariableName, pathVariableType) ->

        val pathVariableAnnotation = AnnotationSpec.builder(PathVariable::class)
          .addMember("value=%S", pathVariableName)
          .build()

        val apiInterfacePathVariableParamBuilder = ParameterSpec.builder(pathVariableName, pathVariableType)
        if (separateFeign) {
          feignApiFuncBuilder.addParameter(
            ParameterSpec.builder(pathVariableName, pathVariableType)
              .addAnnotation(pathVariableAnnotation)
              .build()
          )
        } else {
          apiInterfacePathVariableParamBuilder.addAnnotation(pathVariableAnnotation)
        }

        apiFuncBuilder.addParameter(apiInterfacePathVariableParamBuilder.build())

        webMvcFuncBuilder.addParameter(
          ParameterSpec.builder(pathVariableName, pathVariableType)
            .addAnnotation(pathVariableAnnotation)
            .build()
        )
        serviceFuncBuilder.addParameter(
          ParameterSpec.builder(pathVariableName, pathVariableType).build()
        )
        servicePostProcessingFuncBuilder?.addParameter(
          ParameterSpec.builder(pathVariableName, pathVariableType).build()
        )
        paramsToCallServiceInterface.add(pathVariableName)
      }

      func.requestType
        ?.generic(*func.requestTypeGenericArguments.toTypedArray())
        ?.also { reqType ->
          val apiInterfaceRequestBodyParamBuilder = ParameterSpec.builder(func.requestName, reqType)

          val requestBodyParamAnnoationClass = when (func.method) {
            RequestMethod.POST, RequestMethod.PUT -> RequestBody::class
            else -> SpringQueryMap::class
          }
          if (separateFeign) {
            feignApiFuncBuilder.addParameter(
              ParameterSpec.builder(func.requestName, reqType)
                .addAnnotation(
                  requestBodyParamAnnoationClass
                )
                .build()
            )
          } else {
            apiInterfaceRequestBodyParamBuilder.addAnnotation(requestBodyParamAnnoationClass)
          }

          apiFuncBuilder.addParameter(apiInterfaceRequestBodyParamBuilder.build())
          webMvcFuncBuilder.addParameter(
            ParameterSpec.builder(func.requestName, reqType)
              .also { paramSpec ->
                if (func.method in listOf(RequestMethod.POST, RequestMethod.PUT)) {
                  paramSpec.addAnnotation(RequestBody::class)
                }
              }
              .addAnnotation(
                Valid::class
              )
              .build()
          )
          serviceFuncBuilder.addParameter(
            ParameterSpec.builder(func.requestName, reqType).build()
          )
          servicePostProcessingFuncBuilder?.addParameter(
            ParameterSpec.builder(func.requestName, reqType).build()
          )
          paramsToCallServiceInterface.add(func.requestName)
        }

      val resType = func.responseType
        ?.generic(*func.responseTypeGenericArguments.toTypedArray())
        ?.also { resType ->
          apiFuncBuilder.returns(resType)
          feignApiFuncBuilder.returns(resType)
          webMvcFuncBuilder.returns(resType)
          serviceFuncBuilder.returns(resType)
        }

      func.authenticationDetailType?.let { authenticationDetailType ->
        serviceFuncBuilder.addParameter(func.authenticationDetailName, authenticationDetailType)
        servicePostProcessingFuncBuilder?.addParameter(func.authenticationDetailName, authenticationDetailType)
        paramsToCallServiceInterface.add(func.authenticationDetailName)

        webMvcFuncBuilder.addStatement(
          "val ${func.authenticationDetailName} = %M<%T>()",
          MemberName("zygarde.security.extension.SpringSecurityExtensions", "currentAuthenticationDetail"),
          authenticationDetailType,
        )
      }

      webMvcFuncBuilder.addStatement(
        "val service = %M<%T>()",
        beanFunc,
        ClassName(serviceInterfacePackage, serviceInterfaceName)
      )

      webMvcFuncBuilder.addStatement(
        "val result = service.$serviceFunctionName(${paramsToCallServiceInterface.joinToString(",")})",
      )

      if (servicePostProcessingFuncBuilder != null) {
        val paramsToCallServicePostProcessing = LinkedList(paramsToCallServiceInterface)
        if (resType != null) {
          paramsToCallServicePostProcessing.add("result")
        }
        webMvcFuncBuilder.addStatement(
          "service.${serviceFunctionName}PostProcessing(${paramsToCallServicePostProcessing.joinToString(",")})"
        )
      }

      if (resType != null) {
        servicePostProcessingFuncBuilder?.addParameter("result", resType)
        webMvcFuncBuilder.addStatement("return result")
      }

      apiInterfaceBuilder.addFunction(apiFuncBuilder.build())
      feignApiInterfaceBuilder.addFunction(feignApiFuncBuilder.build())
      webMvcControllerBuilder.addFunction(webMvcFuncBuilder.build())
      serviceInterfaceBuilder.addFunction(serviceFuncBuilder.build())
      servicePostProcessingFuncBuilder?.build()?.let(serviceInterfaceBuilder::addFunction)
    }
  }
}
