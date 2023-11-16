package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.ZyApi
import zygarde.codegen.ZygardeApiGeneratorTargetFolder
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.generator.WebMvcApiGenerator
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.data.api.PageDto
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeApiGenerator(
  processingEnv: ProcessingEnvironment,
  val apiGroupToGenerateTargetFolder: Map<String, ZygardeApiGeneratorTargetFolder> = emptyMap(),
) : AbstractZygardeGenerator(processingEnv) {

  data class ApiPathVariableVo(
    val value: String,
    val type: TypeName
  )

  private val dtoPackage = packageName("data.dto")
  private val servicePackage = packageName("service")
  private val apiPackage = packageName("api")
  private val apiImplPackage = packageName("api.impl")

  fun generateApi(elements: Collection<Element>) {

    elements.mapNotNull { it.getAnnotation(ZyApi::class.java) }.groupBy { it.group }.forEach { (apiGroup, zygardeApis) ->
      val apiVoMap = mutableMapOf<String, ApiToGenerateVo>()
      for (zygardeApi in zygardeApis) {
        zygardeApi.api.forEach { genApi ->
          val (apiName, apiOperation) = genApi.api.split(".").let { it[0] to it[1] }
          val (serviceName, serviceMethod) = genApi.service.split(".").let { it[0] to it[1] }
          val reqRefTypeName = if (genApi.reqRef.isEmpty()) {
            safeGetTypeFromAnnotation { genApi.reqRefClass.asTypeName() }.kotlin(false)
              .takeIf { it.toString() != "kotlin.Any" }
          } else {
            ClassName(dtoPackage, genApi.reqRef)
          }

          val resRefTypeName = if (genApi.resRef.isEmpty()) {
            safeGetTypeFromAnnotation { genApi.resRefClass.asTypeName() }.kotlin(false)
              .takeIf { it.toString() != "kotlin.Any" }
          } else {
            ClassName(dtoPackage, genApi.resRef)
          }

          val pathVariableList = genApi.pathVariable.map { pathVariable ->
            ApiPathVariableVo(
              pathVariable.value,
              safeGetTypeFromAnnotation { pathVariable.type.asTypeName() }.kotlin(false)
            )
          }

          val authenticationDetailType = safeGetTypeFromAnnotation { genApi.authenticationDetail.asTypeName() }
            .kotlin(false)
            .takeIf { it.toString() != "kotlin.Any" }

          val servicePostProcessingParamType = safeGetTypeFromAnnotation { genApi.servicePostProcessingParam.asTypeName() }
            .kotlin(false)
            .takeIf { it.toString() != "kotlin.Any" }

          val apiFunctionVo = ApiFunctionToGenerateVo(
            method = genApi.method,
            functionName = apiOperation,
            description = genApi.apiDescription,
            path = genApi.path,
            pathVariables = pathVariableList.associate {
              it.value to it.type
            },
            requestName = "req",
            requestType = if (genApi.reqCollection) {
              Collection::class.asTypeName()
            } else reqRefTypeName,
            requestTypeGenericArguments = listOfNotNull(reqRefTypeName.takeIf { genApi.reqCollection }),
            responseType = if (genApi.resCollection) {
              Collection::class.asTypeName()
            } else if (genApi.resPage) {
              PageDto::class.asTypeName()
            } else {
              resRefTypeName
            },
            responseTypeGenericArguments = listOfNotNull(resRefTypeName.takeIf { genApi.resCollection || genApi.resPage }),
            serviceName = serviceName,
            serviceFunctionName = serviceMethod,
            postProcessing = genApi.servicePostProcessing,
            postProcessingParamType = servicePostProcessingParamType,
            authenticationDetailName = "authenticationDetail",
            authenticationDetailType = authenticationDetailType,
            deprecated = Deprecated(
              message = genApi.deprecatedMessage,
              replaceWith = genApi.deprecatedReplacement,
              level = DeprecationLevel.WARNING,
            ).takeIf { genApi.deprecated },
          )

          val apiVo = apiVoMap.getOrPut(apiName) {
            ApiToGenerateVo(
              apiInterfacePackage = apiPackage,
              controllerPackage = apiImplPackage,
              serviceInterfacePackage = servicePackage,
              apiName = apiName,
              basePath = null,
              functions = mutableListOf(),
              separateFeign = true,
            )
          }

          apiVo.functions.add(apiFunctionVo)
        }
      }

      val result = WebMvcApiGenerator(apiVoMap.values).generateApis()
      val getFolderToGenerate = fun(resolveFoldertarget: (ZygardeApiGeneratorTargetFolder) -> String?): File {
        val defaultKaptFolder = folderToGenerate()
        return if (apiGroup.isEmpty()) {
          defaultKaptFolder
        } else {
          apiGroupToGenerateTargetFolder[apiGroup]
            ?.let { resolveFoldertarget(it) }
            ?.let { File(it) }
            ?: defaultKaptFolder
        }
      }
      result.apiInterfaces.forEach { it.writeTo(getFolderToGenerate(ZygardeApiGeneratorTargetFolder::generateApiInterfacesTo)) }
      result.feignApiInterfaces.forEach { it.writeTo(getFolderToGenerate(ZygardeApiGeneratorTargetFolder::generateFeignApiInterfacesTo)) }
      result.controllers.forEach { it.writeTo(getFolderToGenerate(ZygardeApiGeneratorTargetFolder::generateControllersTo)) }
      result.serviceInterfaces.forEach { it.writeTo(getFolderToGenerate(ZygardeApiGeneratorTargetFolder::generateServiceInterfacesTo)) }
    }
  }
}
