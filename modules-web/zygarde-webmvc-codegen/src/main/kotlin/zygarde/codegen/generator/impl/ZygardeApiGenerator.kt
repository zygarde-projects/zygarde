package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
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
import zygarde.codegen.ZyApi
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.data.api.PageDto
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.validation.Valid

class ZygardeApiGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  data class ApiPathVariableVo(
    val value: String,
    val type: TypeName
  )

  data class GenApiDescriptionVo(
    val element: Element,
    val method: RequestMethod,
    val path: String,
    val pathVariable: Collection<ApiPathVariableVo>,
    val apiName: String,
    val apiOperation: String,
    val apiDescription: String = "",
    val serviceName: String,
    val serviceMethod: String,
    val servicePostProcessing: Boolean,
    val reqRef: TypeName?,
    val resRef: TypeName?,
    val authenticationDetail: TypeName?,
  )

  private val dtoPackage = packageName("data.dto")
  private val servicePackage = packageName("service")
  private val apiPackage = packageName("api")
  private val apiImplPackage = packageName("api.impl")

  private val beanFunc = MemberName("zygarde.core.di.DiServiceContext", "bean")

  fun generateApi(elements: Collection<Element>) {
    val apis = elements.flatMap { elem ->
      elem.getAnnotation(ZyApi::class.java)?.let { zygardeApi ->
        zygardeApi.api.map { genApi ->
          val (apiName, apiOperation) = genApi.api.split(".").let { it[0] to it[1] }
          val (serviceName, serviceMethod) = genApi.service.split(".").let { it[0] to it[1] }
          val reqRef = if (genApi.reqRef.isEmpty()) {
            safeGetTypeFromAnnotation { genApi.reqRefClass.asTypeName() }.kotlin(false)
              .takeIf { it.toString() != "java.lang.Object" }
              ?.let {
                if (genApi.reqCollection) {
                  Collection::class.generic(it)
                } else {
                  it
                }
              }
          } else {
            ClassName(dtoPackage, genApi.reqRef).let {
              if (genApi.reqCollection) {
                Collection::class.generic(it)
              } else {
                it
              }
            }
          }
          val resRefTypeName = if (genApi.resRef.isEmpty()) {
            safeGetTypeFromAnnotation { genApi.resRefClass.asTypeName() }.kotlin(false)
              .takeIf { it.toString() != "java.lang.Object" }
          } else {
            ClassName(dtoPackage, genApi.resRef)
          }
          val resRef = resRefTypeName?.let {
            when {
              genApi.resCollection -> {
                Collection::class.generic(it)
              }
              genApi.resPage -> {
                PageDto::class.generic(it)
              }
              else -> {
                it
              }
            }
          }
          GenApiDescriptionVo(
            elem,
            method = genApi.method,
            path = genApi.path,
            pathVariable = genApi.pathVariable.map { pathVariable ->
              ApiPathVariableVo(
                pathVariable.value,
                safeGetTypeFromAnnotation { pathVariable.type.asTypeName() }.kotlin(false)
              )
            },
            apiName = apiName,
            apiOperation = apiOperation,
            apiDescription = genApi.apiDescription,
            serviceName = serviceName,
            serviceMethod = serviceMethod,
            servicePostProcessing = genApi.servicePostProcessing,
            reqRef = reqRef,
            resRef = resRef,
            authenticationDetail = safeGetTypeFromAnnotation { genApi.authenticationDetail.asTypeName() }.kotlin(false)
              .takeIf { it.toString() != "java.lang.Object" },
          )
        }
      } ?: emptyList()
    }

    generateServiceInterface(apis)
    generateApiInterface(apis)
    generateApiImpl(apis)
  }

  private fun generateServiceInterface(apis: List<GenApiDescriptionVo>) {
    apis.groupBy { it.serviceName }.forEach { (serviceName, apisSameServiceName) ->
      val serviceFileBuilder = FileSpec.builder(servicePackage, serviceName)
      val serviceInterfaceBuilder = TypeSpec.interfaceBuilder(serviceName)
      apisSameServiceName.groupBy { api ->
        val pageVariables = api.pathVariable.sortedBy { it.value }.joinToString("_")
        "${api.serviceMethod}_${api.reqRef}_${api.resRef}_$pageVariables"
      }.forEach { (_, apisSameMethod) ->
        val api = apisSameMethod.first()
        val methodName = api.serviceMethod
        serviceInterfaceBuilder.addFunction(
          FunSpec.builder(methodName)
            .addModifiers(KModifier.ABSTRACT)
            .also { fb ->
              api.pathVariable.forEach {
                fb.addParameter(it.value, it.type)
              }
              if (api.reqRef != null) {
                fb.addParameter("req", api.reqRef)
              }
              if (api.authenticationDetail != null) {
                fb.addParameter(
                  ParameterSpec("authenticationDetail", api.authenticationDetail)
                )
              }
              if (api.resRef != null) {
                fb.returns(api.resRef)
              }
            }
            .build()
        )
        if (apisSameMethod.any { it.servicePostProcessing }) {
          serviceInterfaceBuilder.addFunction(
            FunSpec.builder(methodName + "PostProcessing")
              .addModifiers(KModifier.ABSTRACT)
              .also { fb ->
                api.pathVariable.forEach {
                  fb.addParameter(it.value, it.type)
                }
                if (api.reqRef != null) {
                  fb.addParameter("req", api.reqRef)
                }
                if (api.authenticationDetail != null) {
                  fb.addParameter(
                    ParameterSpec("authenticationDetail", api.authenticationDetail)
                  )
                }
                if (api.resRef != null) {
                  fb.addParameter("result", api.resRef)
                }
              }
              .build()
          )
        }
      }
      serviceFileBuilder.addType(serviceInterfaceBuilder.build())
        .build()
        .writeTo(folderToGenerate())
    }
  }

  private fun generateApiInterface(apis: List<GenApiDescriptionVo>) {
    apis.groupBy { it.apiName }.forEach { (apiName, apisSameApiName) ->
      val apiFileBuilder = FileSpec.builder(apiPackage, apiName)
      val apiInterfaceBuilder = TypeSpec.interfaceBuilder(apiName)
        .addAnnotation(
          AnnotationSpec.builder(FeignClient::class)
            .addMember("name=%S", apiName)
            .build()
        )
        .addAnnotation(
          AnnotationSpec.builder(Tag::class)
            .addMember("name=%S", apiName)
            .build()
        )
      apisSameApiName.forEach { api ->
        apiInterfaceBuilder.addFunction(
          FunSpec.builder(api.apiOperation)
            .addModifiers(KModifier.ABSTRACT)
            .also { fb ->
              fb.addAnnotation(
                AnnotationSpec
                  .builder(
                    when (api.method) {
                      RequestMethod.POST -> PostMapping::class
                      RequestMethod.PUT -> PutMapping::class
                      RequestMethod.DELETE -> DeleteMapping::class
                      else -> GetMapping::class
                    }
                  )
                  .addMember("value=[%S]", api.path)
                  .build()
              )
              fb.addAnnotation(
                AnnotationSpec.builder(Operation::class)
                  .addMember("summary=%S", "${api.apiOperation} ${api.apiDescription}")
                  .build()
              )
              api.pathVariable.forEach { pv ->
                fb.addParameter(
                  ParameterSpec.builder(pv.value, pv.type)
                    .addAnnotation(
                      AnnotationSpec.builder(PathVariable::class)
                        .addMember("value=%S", pv.value)
                        .build()
                    )
                    .build()
                )
              }
              if (api.reqRef != null) {
                fb.addParameter(
                  ParameterSpec.builder("req", api.reqRef)
                    .addAnnotation(
                      if (api.method == RequestMethod.GET) {
                        SpringQueryMap::class
                      } else {
                        RequestBody::class
                      }
                    )
                    .addAnnotation(
                      Valid::class
                    )
                    .build()
                )
              }
              if (api.resRef != null) {
                fb.returns(api.resRef)
              }
            }
            .build()
        )
      }
      apiFileBuilder.addType(apiInterfaceBuilder.build())
        .build()
        .writeTo(folderToGenerate())
    }
  }

  private fun generateApiImpl(apis: List<GenApiDescriptionVo>) {
    apis.groupBy { it.apiName }.forEach { (apiName, apisSameApiName) ->
      val apiImplName = "${apiName}Impl"
      val apiImplFileBuilder = FileSpec.builder(apiImplPackage, apiImplName)
      val apiImplClass = TypeSpec.classBuilder(apiImplName)
        .addAnnotation(RestController::class)
        .addSuperinterface(ClassName(apiPackage, apiName))
      apisSameApiName.forEach { api ->
        apiImplClass.addFunction(
          FunSpec.builder(api.apiOperation)
            .addModifiers(KModifier.OVERRIDE)
            .also { fb ->
              val paramsToCallServiceInterface = mutableListOf<String>()
              api.pathVariable.forEach { pv ->
                paramsToCallServiceInterface.add(pv.value)
                fb.addParameter(
                  ParameterSpec.builder(pv.value, pv.type)
                    .build()
                )
              }
              if (api.reqRef != null) {
                paramsToCallServiceInterface.add("req")
                fb.addParameter(
                  ParameterSpec.builder("req", api.reqRef)
                    .also {
                      if (api.method == RequestMethod.POST || api.method == RequestMethod.PUT) {
                        it.addAnnotation(
                          RequestBody::class
                        )
                      }
                    }
                    .addAnnotation(
                      Valid::class
                    )
                    .build()
                )
              }

              if (api.resRef != null) {
                fb.returns(api.resRef)
              }

              if (api.authenticationDetail != null) {
                paramsToCallServiceInterface.add("authenticationDetail")
              }

              val codeBlockArgs = mutableListOf<Any>(
                beanFunc,
                ClassName(servicePackage, api.serviceName)
              )

              val postProcessingStatement = if (api.servicePostProcessing) {
                val paramsForPostProcessing = paramsToCallServiceInterface.toMutableList().also {
                  it.add("result")
                }
                codeBlockArgs.add(beanFunc)
                codeBlockArgs.add(ClassName(servicePackage, api.serviceName))
                "\r\n\t\t.also{ result -> %M<%T>().${api.serviceMethod}PostProcessing(${paramsForPostProcessing.joinToString(",")}) }"
              } else {
                ""
              }

              if (api.authenticationDetail != null) {
                fb.addStatement(
                  "val authenticationDetail = %M<%T>()",
                  MemberName("zygarde.security.extension.SpringSecurityExtensions", "currentAuthenticationDetail"),
                  api.authenticationDetail
                )
              }

              fb.addStatement(
                "return %M<%T>()\r\n\t\t.${api.serviceMethod}(${paramsToCallServiceInterface.joinToString(",")})$postProcessingStatement",
                *codeBlockArgs.toTypedArray()
              )
            }
            .build()
        )
      }

      apiImplFileBuilder.addType(apiImplClass.build())
        .build()
        .writeTo(folderToGenerate())
    }
  }
}
