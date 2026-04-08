package zygarde.codegen.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ksp.ZygardeWebMvcKspOptions.API_STATIC_OPTION_PACKAGE
import zygarde.codegen.ksp.ZygardeWebMvcKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.extension.fieldName
import zygarde.codegen.ksp.extension.getArgumentValueAsString
import zygarde.codegen.ksp.extension.implementsInterface
import zygarde.codegen.ksp.extension.name
import zygarde.core.props.ZygardeApiProperties
import zygarde.data.option.OptionDto
import zygarde.data.option.OptionEnum

class ZygardeStaticOptionApiKspProcessor(
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

  private val optionPackage: String by lazy {
    packageName(options.getOrDefault(API_STATIC_OPTION_PACKAGE, "api.option"))
  }
  private val optionDtoPackage by lazy { "$optionPackage.dto" }
  private val optionDtoName = "StaticOptionDto"
  private val optionApiName = "StaticOptionApi"
  private val optionControllerPackage by lazy { "$optionPackage.impl" }
  private val optionControllerName = "StaticOptionController"

  private val optionDtoCollectionType = Collection::class.asClassName().parameterizedBy(OptionDto::class.asClassName())

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) {
      return emptyList()
    }
    invoked = true

    val staticOptionApiAnnotationName = StaticOptionApi::class.qualifiedName ?: return emptyList()

    val elements = resolver.getSymbolsWithAnnotation(staticOptionApiAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.implementsInterface(OptionEnum::class.qualifiedName!!) }
      .sortedBy { it.name() }
      .toList()

    if (elements.isEmpty()) {
      return emptyList()
    }

    generateStaticOptionDto(elements).writeTo(codeGenerator, aggregating = false)
    generateStaticOptionApiInterface(elements).writeTo(codeGenerator, aggregating = false)
    generateStaticOptionController(elements).writeTo(codeGenerator, aggregating = false)

    return emptyList()
  }

  private fun generateStaticOptionDto(elements: List<KSClassDeclaration>): FileSpec {
    val fileSpec = FileSpec.builder(optionDtoPackage, optionDtoName)
    val staticOptionDtoBuilder = TypeSpec.classBuilder(optionDtoName)
      .addModifiers(KModifier.DATA)
      .addAnnotation(Schema::class)
    val constructorBuilder = FunSpec.constructorBuilder()

    elements.forEach { elem ->
      val staticOptionApiAnn = elem.annotations.find { it.shortName.asString() == "StaticOptionApi" }
      val comment = staticOptionApiAnn?.getArgumentValueAsString("comment") ?: ""
      val elemTypeName = elem.asType(emptyList()).toTypeName()

      ParameterSpec
        .builder(elem.fieldName(), optionDtoCollectionType)
        .defaultValue(
          CodeBlock.builder()
            .addStatement("%T.values().map{ it.toOptionDto() }", elemTypeName)
            .build()
        )
        .build().also { constructorBuilder.addParameter(it) }
      PropertySpec
        .builder(elem.fieldName(), optionDtoCollectionType)
        .initializer(elem.fieldName())
        .addAnnotation(
          AnnotationSpec.builder(Schema::class)
            .addMember("description=%S", comment)
            .build()
        ).build().also { staticOptionDtoBuilder.addProperty(it) }
    }

    return fileSpec
      .addType(
        staticOptionDtoBuilder.primaryConstructor(constructorBuilder.build()).build()
      )
      .build()
  }

  private fun generateStaticOptionApiInterface(elements: List<KSClassDeclaration>): FileSpec {
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
      val staticOptionApiAnn = elem.annotations.find { it.shortName.asString() == "StaticOptionApi" }
      val comment = staticOptionApiAnn?.getArgumentValueAsString("comment") ?: ""

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
                .addMember("summary=%S", "Get $comment")
                .build()
            )
            .returns(optionDtoCollectionType)
            .build()
        )
    }

    return fileSpec
      .addType(staticOptionApiBuilder.build())
      .build()
  }

  private fun generateStaticOptionController(elements: List<KSClassDeclaration>): FileSpec {
    val fileSpec = FileSpec.builder(optionControllerPackage, optionControllerName)
    val zygardeApiPropertiesType = ZygardeApiProperties::class.asClassName()
    val dtoType = ClassName(optionDtoPackage, optionDtoName)

    val lazyBlock = CodeBlock.builder()
      .beginControlFlow("lazy")
      .addStatement("val activeOverrides = zygardeApiProperties.staticOptionApi.active")
      .add("%T(\n", dtoType)
    elements.forEachIndexed { index, elem ->
      val elemTypeName = elem.asType(emptyList()).toTypeName()
      lazyBlock.add(
        "  %L = %T.values().map { it.toOptionDto(activeOverrides[%S]) }",
        elem.fieldName(),
        elemTypeName,
        elem.name()
      )
      if (index < elements.size - 1) {
        lazyBlock.add(",\n")
      } else {
        lazyBlock.add("\n")
      }
    }
    lazyBlock.add(")\n")
    lazyBlock.endControlFlow()

    val staticOptionControllerBuilder = TypeSpec.classBuilder(optionControllerName)
      .addSuperinterface(ClassName(optionPackage, optionApiName))
      .addAnnotation(RestController::class)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("zygardeApiProperties", zygardeApiPropertiesType)
          .build()
      )
      .addProperty(
        PropertySpec.builder("zygardeApiProperties", zygardeApiPropertiesType, KModifier.PRIVATE)
          .initializer("zygardeApiProperties")
          .build()
      )
      .addProperty(
        PropertySpec.builder("staticOptionDto", dtoType, KModifier.PRIVATE)
          .delegate(lazyBlock.build())
          .build()
      )
      .addFunction(
        FunSpec.builder("getAllStaticOptions")
          .addModifiers(KModifier.OVERRIDE)
          .returns(dtoType)
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
