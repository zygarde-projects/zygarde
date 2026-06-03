package zygarde.codegen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import zygarde.core.props.ZygardeApiProperties
import zygarde.data.option.OptionDto

data class StaticOptionApiMeta(
  val enumName: String,
  val enumTypeName: TypeName,
  val comment: String,
  val key: String,
  val path: String,
  val sourceName: String = enumName,
)

class StaticOptionApiGeneratorSupport(
  private val optionPackage: String,
) {
  private val optionDtoPackage = "$optionPackage.dto"
  private val optionDtoName = "StaticOptionDto"
  private val optionApiName = "StaticOptionApi"
  private val optionControllerPackage = "$optionPackage.impl"
  private val optionControllerName = "StaticOptionController"
  private val optionDtoCollectionType = Collection::class.asClassName().parameterizedBy(OptionDto::class.asClassName())

  fun generateStaticOptionDto(elements: Collection<StaticOptionApiMeta>): FileSpec {
    val metas = elements.normalized()
    val fileSpec = FileSpec.builder(optionDtoPackage, optionDtoName)
    val staticOptionDtoBuilder = TypeSpec.classBuilder(optionDtoName)
      .addModifiers(KModifier.DATA)
      .addAnnotation(Schema::class)
    val constructorBuilder = FunSpec.constructorBuilder()

    metas.forEach { meta ->
      ParameterSpec
        .builder(meta.propertyName, optionDtoCollectionType)
        .defaultValue(
          CodeBlock.builder()
            .addStatement("%T.values().map { it.toOptionDto() }", meta.enumTypeName)
            .build()
        )
        .build().also { constructorBuilder.addParameter(it) }
      PropertySpec
        .builder(meta.propertyName, optionDtoCollectionType)
        .initializer(meta.propertyName)
        .addAnnotation(
          AnnotationSpec.builder(Schema::class)
            .addMember("description=%S", meta.comment)
            .build()
        )
        .build()
        .also { staticOptionDtoBuilder.addProperty(it) }
    }

    return fileSpec
      .addType(staticOptionDtoBuilder.primaryConstructor(constructorBuilder.build()).build())
      .build()
  }

  fun generateStaticOptionApiInterface(elements: Collection<StaticOptionApiMeta>): FileSpec {
    val metas = elements.normalized()
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
              .addMember("""value=["$springPropertyPath"]""")
              .build()
          )
          .addAnnotation(
            AnnotationSpec.builder(Operation::class)
              .addMember("summary=%S", "Get All Static Options")
              .build()
          )
          .returns(ClassName(optionDtoPackage, optionDtoName))
          .build()
      )

    metas.forEach { meta ->
      staticOptionApiBuilder
        .addFunction(
          FunSpec.builder(meta.functionName)
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(
              AnnotationSpec.builder(GetMapping::class)
                .addMember("""value=["$springPropertyPath/${meta.pathSegment}"]""")
                .build()
            )
            .addAnnotation(
              AnnotationSpec.builder(Operation::class)
                .addMember("summary=%S", "Get ${meta.comment.ifBlank { meta.optionKey }}")
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

  fun generateStaticOptionController(elements: Collection<StaticOptionApiMeta>): FileSpec {
    val metas = elements.normalized()
    val fileSpec = FileSpec.builder(optionControllerPackage, optionControllerName)
    val zygardeApiPropertiesType = ZygardeApiProperties::class.asClassName()
    val dtoType = ClassName(optionDtoPackage, optionDtoName)

    val lazyBlock = CodeBlock.builder()
      .beginControlFlow("lazy")
      .addStatement("val activeOverrides = zygardeApiProperties.staticOptionApi.active")
      .add("%T(\n", dtoType)
    metas.forEachIndexed { index, meta ->
      lazyBlock.add(
        "  %L = %T.values().map { it.toOptionDto(activeOverrides[%S]) }",
        meta.propertyName,
        meta.enumTypeName,
        meta.optionKey,
      )
      if (index < metas.size - 1) {
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

    metas.forEach { meta ->
      staticOptionControllerBuilder
        .addFunction(
          FunSpec.builder(meta.functionName)
            .addModifiers(KModifier.OVERRIDE)
            .returns(optionDtoCollectionType)
            .addStatement("return staticOptionDto.${meta.propertyName}")
            .build()
        )
    }

    return fileSpec
      .addType(staticOptionControllerBuilder.build())
      .build()
  }

  private fun Collection<StaticOptionApiMeta>.normalized(): List<ResolvedStaticOptionApiMeta> {
    val metas = sortedBy { it.enumName }
      .map {
        val optionKey = it.key.ifBlank { it.enumName }
        val propertyName = if (it.key.isBlank()) {
          it.enumName.replaceFirstChar { c -> c.lowercase() }
        } else {
          optionKey.toIdentifier(lowerCamel = true)
        }
        val pathSegment = it.path.ifBlank {
          if (it.key.isBlank()) {
            propertyName
          } else {
            optionKey
          }
        }
        ResolvedStaticOptionApiMeta(
          enumName = it.enumName,
          sourceName = it.sourceName,
          enumTypeName = it.enumTypeName,
          comment = it.comment,
          optionKey = optionKey,
          propertyName = propertyName,
          functionName = "get${optionKey.toIdentifier(lowerCamel = false)}",
          pathSegment = pathSegment.trim('/'),
        )
      }

    requireUnique(metas, ResolvedStaticOptionApiMeta::optionKey, "static option key")
    requireUnique(metas, ResolvedStaticOptionApiMeta::propertyName, "generated StaticOptionDto property")
    requireUnique(metas, ResolvedStaticOptionApiMeta::functionName, "generated StaticOptionApi function")
    requireUnique(metas, ResolvedStaticOptionApiMeta::pathSegment, "static option endpoint path")

    return metas
  }

  private fun requireUnique(
    metas: List<ResolvedStaticOptionApiMeta>,
    selector: (ResolvedStaticOptionApiMeta) -> String,
    label: String,
  ) {
    val duplicated = metas.groupBy(selector).filterValues { it.size > 1 }
    require(duplicated.isEmpty()) {
      duplicated.entries.joinToString(prefix = "Duplicate $label: ") { (key, items) ->
        "$key used by ${items.joinToString { it.sourceName }}"
      }
    }
  }

  private fun String.toIdentifier(lowerCamel: Boolean): String {
    val words = split(Regex("[^A-Za-z0-9]+"))
      .filter { it.isNotBlank() }
    val raw = if (words.isEmpty()) {
      filter { it.isLetterOrDigit() }
    } else {
      words.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
    val safe = raw.ifBlank { "option" }
      .let { if (it.first().isDigit()) "Option$it" else it }

    return if (lowerCamel) {
      safe.replaceFirstChar { it.lowercase() }
    } else {
      safe.replaceFirstChar { it.uppercase() }
    }
  }

  private data class ResolvedStaticOptionApiMeta(
    val enumName: String,
    val sourceName: String,
    val enumTypeName: TypeName,
    val comment: String,
    val optionKey: String,
    val propertyName: String,
    val functionName: String,
    val pathSegment: String,
  )
}
