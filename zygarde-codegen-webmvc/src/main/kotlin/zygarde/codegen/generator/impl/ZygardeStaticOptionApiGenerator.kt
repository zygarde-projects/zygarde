package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ZygardeKaptOptions.Companion.API_STATIC_OPTION_PACKAGE
import zygarde.codegen.extension.kotlinpoet.*
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.data.option.OptionDto
import zygarde.data.option.OptionEnum
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeStaticOptionApiGenerator(
  processingEnv: ProcessingEnvironment
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

  fun generateStaticOptionApi(elements: Collection<Element>) {
    val filtered = elements.filter { it.isTypeOf<OptionEnum>() }
    if (filtered.isEmpty()) {
      return
    }
    generateStaticOptionDto(filtered)
    generateStaticOptionApi()
    generateStaticOptionController()
  }

  private fun generateStaticOptionDto(elements: Collection<Element>) {
    val fileSpec = FileSpec.builder(optionDtoPackage, optionDtoName)
    val staticOptionDtoBuilder = TypeSpec.classBuilder(optionDtoName)
      .addModifiers(KModifier.DATA)
      .addAnnotation(Schema::class)
    val constructorBuilder = FunSpec.constructorBuilder()
    elements.forEach { elem ->
      val staticOptionApi = elem.getAnnotation(StaticOptionApi::class.java)
      val propType = Collection::class.asClassName().parameterizedBy(OptionDto::class.asClassName())
      ParameterSpec
        .builder(elem.fieldName(), propType)
        .defaultValue(
          CodeBlock.builder()
            .addStatement("%T.values().map{ it.toOptionDto() }", elem.asType())
            .build()
        )
        .build().also { constructorBuilder.addParameter(it) }
      PropertySpec
        .builder(elem.fieldName(), propType)
        .initializer(elem.fieldName())
        .addAnnotation(
          AnnotationSpec.builder(Schema::class)
            .addMember("description=%S", staticOptionApi.comment)
            .build()
        ).build().also { staticOptionDtoBuilder.addProperty(it) }
    }
    fileSpec
      .addType(
        staticOptionDtoBuilder.primaryConstructor(constructorBuilder.build()).build()
      )
      .build()
      .writeTo(folderToGenerate())
  }

  private fun generateStaticOptionApi() {
    val fileSpec = FileSpec.builder(optionPackage, optionApiName)
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
      .addAnnotation(
        AnnotationSpec.builder(RequestMapping::class)
          .addMember(
            """value=["\_*zygarde.api.static-option-api.path}"]"""
              .replace("_", "$")
              .replace("*", "{")
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("getStaticOptions")
          .addModifiers(KModifier.ABSTRACT)
          .addAnnotation(GetMapping::class)
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
    fileSpec
      .addType(staticOptionApiBuilder.build())
      .build()
      .writeTo(folderToGenerate())
  }

  private fun generateStaticOptionController() {
    val fileSpec = FileSpec.builder(optionControllerPackage, optionControllerName)
    val staticOptionControllerBuilder = TypeSpec.classBuilder(optionControllerName)
      .addSuperinterface(
        ClassName(optionPackage, optionApiName)
      )
      .addAnnotation(RestController::class)
      .addFunction(
        FunSpec.builder("getStaticOptions")
          .addModifiers(KModifier.OVERRIDE)
          .returns(
            ClassName(optionDtoPackage, optionDtoName)
          )
          .addStatement("return %T()", ClassName(optionDtoPackage, optionDtoName))
          .build()
      )
    fileSpec
      .addType(staticOptionControllerBuilder.build())
      .build()
      .writeTo(folderToGenerate())
  }
}
