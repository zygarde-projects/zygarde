package zygarde.codegen.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.ZyApi
import zygarde.codegen.generator.WebMvcApiGenerator
import zygarde.codegen.ksp.ZygardeWebMvcKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.extension.getArgumentValueAsAnnotationList
import zygarde.codegen.ksp.extension.getArgumentValueAsBoolean
import zygarde.codegen.ksp.extension.getArgumentValueAsEnumEntry
import zygarde.codegen.ksp.extension.getArgumentValueAsString
import zygarde.codegen.ksp.extension.getArgumentValueAsTypeName
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.data.api.PageDto

class ZygardeApiKspProcessor(
  private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
  private val codeGenerator: CodeGenerator = environment.codeGenerator
  private val logger: KSPLogger = environment.logger
  private val options: Map<String, String> = environment.options

  private var invoked = false

  private fun packageName(pack: String): String {
    val basePackage = options.getOrDefault(BASE_PACKAGE, "zygarde.generated")
    return "$basePackage.$pack"
  }

  private val dtoPackage by lazy { packageName(options.getOrDefault(ZygardeWebMvcKspOptions.DTO_PACKAGE, "data.dto")) }
  private val servicePackage by lazy { packageName("service") }
  private val apiPackage by lazy { packageName("api") }
  private val apiImplPackage by lazy { packageName("api.impl") }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) {
      return emptyList()
    }
    invoked = true

    val zyApiAnnotationName = ZyApi::class.qualifiedName ?: return emptyList()

    val zyApiSymbols = resolver.getSymbolsWithAnnotation(zyApiAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .toList()

    if (zyApiSymbols.isEmpty()) {
      return emptyList()
    }

    generateApis(zyApiSymbols)

    return emptyList()
  }

  private fun generateApis(elements: List<KSClassDeclaration>) {
    elements
      .sortedBy { it.asType(emptyList()).toTypeName().toString() }
      .mapNotNull { classDecl ->
        classDecl.annotations.find { it.shortName.asString() == "ZyApi" }
      }
      .groupBy { zyApi -> zyApi.getArgumentValueAsString("group") }
      .forEach { (apiGroup, zygardeApis) ->
        val apiVoMap = mutableMapOf<String, ApiToGenerateVo>()

        for (zygardeApi in zygardeApis) {
          val genApis = zygardeApi.getArgumentValueAsAnnotationList("api")

          for (genApi in genApis) {
            val apiStr = genApi.getArgumentValueAsString("api")
            val serviceStr = genApi.getArgumentValueAsString("service")

            if (apiStr.isEmpty() || serviceStr.isEmpty()) continue

            val (apiName, apiOperation) = apiStr.split(".").let { it[0] to it[1] }
            val (serviceName, serviceMethod) = serviceStr.split(".").let { it[0] to it[1] }

            val reqRef = genApi.getArgumentValueAsString("reqRef")
            val reqRefClass = genApi.getArgumentValueAsTypeName("reqRefClass")
            val reqCollection = genApi.getArgumentValueAsBoolean("reqCollection")

            val resRef = genApi.getArgumentValueAsString("resRef")
            val resRefClass = genApi.getArgumentValueAsTypeName("resRefClass")
            val resCollection = genApi.getArgumentValueAsBoolean("resCollection")
            val resPage = genApi.getArgumentValueAsBoolean("resPage")

            val reqRefTypeName = if (reqRef.isEmpty()) {
              reqRefClass?.takeIf { it.toString() != "kotlin.Any" }
            } else {
              ClassName(dtoPackage, reqRef)
            }

            val resRefTypeName = if (resRef.isEmpty()) {
              resRefClass?.takeIf { it.toString() != "kotlin.Any" }
            } else {
              ClassName(dtoPackage, resRef)
            }

            val pathVariables = genApi.getArgumentValueAsAnnotationList("pathVariable")
              .associate { pathVar ->
                val value = pathVar.getArgumentValueAsString("value")
                val type = pathVar.getArgumentValueAsTypeName("type") ?: String::class.asTypeName()
                value to type
              }

            val authenticationDetailType = genApi.getArgumentValueAsTypeName("authenticationDetail")
              ?.takeIf { it.toString() != "kotlin.Any" }

            val servicePostProcessingParamType = genApi.getArgumentValueAsTypeName("servicePostProcessingParam")
              ?.takeIf { it.toString() != "kotlin.Any" }

            val methodStr = genApi.getArgumentValueAsEnumEntry("method") ?: "GET"
            val method = RequestMethod.valueOf(methodStr)

            val deprecated = genApi.getArgumentValueAsBoolean("deprecated")
            val deprecatedMessage = genApi.getArgumentValueAsString("deprecatedMessage")

            val apiFunctionVo = ApiFunctionToGenerateVo(
              method = method,
              functionName = apiOperation,
              description = genApi.getArgumentValueAsString("apiDescription"),
              path = genApi.getArgumentValueAsString("path"),
              pathVariables = pathVariables,
              requestName = "req",
              requestType = if (reqCollection) {
                Collection::class.asTypeName()
              } else {
                reqRefTypeName
              },
              requestTypeGenericArguments = listOfNotNull(reqRefTypeName.takeIf { reqCollection }),
              responseType = if (resCollection) {
                Collection::class.asTypeName()
              } else if (resPage) {
                PageDto::class.asTypeName()
              } else {
                resRefTypeName
              },
              responseTypeGenericArguments = listOfNotNull(resRefTypeName.takeIf { resCollection || resPage }),
              serviceName = serviceName,
              serviceFunctionName = serviceMethod,
              postProcessing = genApi.getArgumentValueAsBoolean("servicePostProcessing"),
              postProcessingParamType = servicePostProcessingParamType,
              authenticationDetailName = "authenticationDetail",
              authenticationDetailType = authenticationDetailType,
              deprecated = Deprecated(
                message = deprecatedMessage,
                replaceWith = ReplaceWith(""),
                level = DeprecationLevel.WARNING,
              ).takeIf { deprecated },
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

        result.apiInterfaces.forEach { it.writeTo(codeGenerator, aggregating = false) }
        result.feignApiInterfaces.forEach { it.writeTo(codeGenerator, aggregating = false) }
        result.controllers.forEach { it.writeTo(codeGenerator, aggregating = false) }
        result.serviceInterfaces.forEach { it.writeTo(codeGenerator, aggregating = false) }
      }
  }
}
