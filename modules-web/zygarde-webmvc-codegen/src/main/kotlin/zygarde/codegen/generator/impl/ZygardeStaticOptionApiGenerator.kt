package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ZygardeApiGeneratorKaptOptions.API_STATIC_OPTION_PACKAGE
import zygarde.codegen.ZygardeStaticOptionApiGeneratorTargetFolder
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.kotlinTypeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.data.option.OptionDto
import zygarde.data.option.OptionEnum
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeStaticOptionApiGenerator(
  processingEnv: ProcessingEnvironment,
  val targetFolderConfig: ZygardeStaticOptionApiGeneratorTargetFolder?,
) : AbstractZygardeGenerator(processingEnv) {

  private val optionPackage: String by lazy {
    packageName(processingEnv.options.getOrDefault(API_STATIC_OPTION_PACKAGE, "api.option"))
  }
  private val optionDtoPackage by lazy {
    "$optionPackage.dto"
  }
  private val optionDtoName = "StaticOptionDto"
  private val optionApiName = "StaticOptionApi"
  private val optionControllerPackage = "$optionPackage.impl"
  private val optionControllerName = "StaticOptionController"

  private val optionDtoCollectionType = Collection::class.asClassName().parameterizedBy(OptionDto::class.asClassName())

  fun generateStaticOptionApi(elements: Collection<Element>) {
    val filtered = elements
      .filter { it.isTypeOf<OptionEnum>() }
      .sortedBy { it.name() }
    if (filtered.isEmpty()) {
      return
    }

    val getFolderToGenerate = fun(resolveFoldertarget: (ZygardeStaticOptionApiGeneratorTargetFolder) -> String?): File {
      val defaultKaptFolder = folderToGenerate()
      return targetFolderConfig
        ?.let { resolveFoldertarget(it) }
        ?.let { File(it) }
        ?: defaultKaptFolder
    }

    generateStaticOptionDto(filtered)
      .writeTo(getFolderToGenerate(ZygardeStaticOptionApiGeneratorTargetFolder::generateDtosTo))
    generateStaticOptionApiInterface(filtered)
      .writeTo(getFolderToGenerate(ZygardeStaticOptionApiGeneratorTargetFolder::generateFeignApiInterfacesTo))
    generateStaticOptionController(filtered)
      .writeTo(getFolderToGenerate(ZygardeStaticOptionApiGeneratorTargetFolder::generateControllersTo))
  }

  private fun generateStaticOptionDto(elements: Collection<Element>): FileSpec {
    val fileSpec = FileSpec.builder(optionDtoPackage, optionDtoName)
    val staticOptionDtoBuilder = TypeSpec.classBuilder(optionDtoName)
      .addModifiers(KModifier.DATA)
      .addAnnotation(Schema::class)
    val constructorBuilder = FunSpec.constructorBuilder()
    elements.forEach { elem ->
      val staticOptionApi = elem.getAnnotation(StaticOptionApi::class.java)
      ParameterSpec
        .builder(elem.fieldName(), optionDtoCollectionType)
        .defaultValue(
          CodeBlock.builder()
            .addStatement("%T.values().map{ it.toOptionDto() }", elem.asType().kotlinTypeName(false))
            .build()
        )
        .build().also { constructorBuilder.addParameter(it) }
      PropertySpec
        .builder(elem.fieldName(), optionDtoCollectionType)
        .initializer(elem.fieldName())
        .addAnnotation(
          AnnotationSpec.builder(Schema::class)
            .addMember("description=%S", staticOptionApi.comment)
            .build()
        ).build().also { staticOptionDtoBuilder.addProperty(it) }
    }
    return fileSpec
      .addType(
        staticOptionDtoBuilder.primaryConstructor(constructorBuilder.build()).build()
      )
      .build()
  }

  private fun generateStaticOptionApiInterface(elements: Collection<Element>): FileSpec {
    val fileSpec = FileSpec.builder(optionPackage, optionApiName)
    val springPropertyPath = """\${'$'}{zygarde.api.static-option-api.path}"""
    val staticOptionApiBuilder = TypeSpec.interfaceBuilder(optionApiName)
      .addAnnotation(
        AnnotationSpec.builder(FeignClient::class)
          .addMember("name=%S", optionApiName)
          .build()
      )
      .addAnnotation(
        AnnotationSpec.builder(Tag::class)
          .addMember("name=%S", optionApiName)
          .build()
      )
      .addFunction(
        FunSpec.builder("getAllStaticOptions")
          .addModifiers(KModifier.ABSTRACT)
          .addAnnotation(
            AnnotationSpec.builder(GetMapping::class)
              .addMember(
                """value=["$springPropertyPath"]"""
              )
              .build()
          )
          .addAnnotation(
            AnnotationSpec.builder(Operation::class)
              .addMember("summary=%S", "Get All Static Options")
              .build()
          )
          .returns(
            ClassName(optionDtoPackage, optionDtoName)
          )
          .build()
      )

    elements.forEach { elem ->
      val staticOptionApi = elem.getAnnotation(StaticOptionApi::class.java)
      staticOptionApiBuilder
        .addFunction(
          FunSpec.builder("get${elem.name()}")
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(
              AnnotationSpec.builder(GetMapping::class)
                .addMember(
                  """value=["$springPropertyPath/${elem.fieldName()}"]"""
                )
                .build()
            )
            .addAnnotation(
              AnnotationSpec.builder(Operation::class)
                .addMember("summary=%S", "Get ${staticOptionApi.comment}")
                .build()
            )
            .returns(
              optionDtoCollectionType
            )
            .build()
        )
    }

    return fileSpec
      .addType(staticOptionApiBuilder.build())
      .build()
  }

  private fun generateStaticOptionController(elements: Collection<Element>): FileSpec {
    val fileSpec = FileSpec.builder(optionControllerPackage, optionControllerName)
    val staticOptionControllerBuilder = TypeSpec.classBuilder(optionControllerName)
      .addSuperinterface(
        ClassName(optionPackage, optionApiName)
      )
      .addAnnotation(RestController::class)
      .addProperty(
        PropertySpec.builder("staticOptionDto", ClassName(optionDtoPackage, optionDtoName), KModifier.PRIVATE)
          .initializer("%T()", ClassName(optionDtoPackage, optionDtoName))
          .build()
      )
      .addFunction(
        FunSpec.builder("getAllStaticOptions")
          .addModifiers(KModifier.OVERRIDE)
          .returns(
            ClassName(optionDtoPackage, optionDtoName)
          )
          .addStatement("return staticOptionDto")
          .build()
      )

    elements.forEach { elem ->
      staticOptionControllerBuilder
        .addFunction(
          FunSpec.builder("get${elem.name()}")
            .addModifiers(KModifier.OVERRIDE)
            .returns(optionDtoCollectionType)
            .addStatement("return staticOptionDto.${elem.fieldName()}")
            .build()
        )
    }

    return fileSpec
      .addType(staticOptionControllerBuilder.build())
      .build()
  }
}
