package zygarde.codegen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
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
    // No constructor and no per-property default expressions: constructor defaults all compile
    // into a single synthetic constructor, which exceeds the JVM 64KB method limit once the
    // annotated enum count grows to ~150. Values are filled by the generated controller.
    val staticOptionDtoBuilder = TypeSpec.classBuilder(optionDtoName)
      .addAnnotation(Schema::class)

    metas.forEach { meta ->
      PropertySpec
        .builder(meta.propertyName, optionDtoCollectionType)
        .mutable()
        .initializer("emptyList()")
        .addAnnotation(
          AnnotationSpec.builder(Schema::class)
            .addMember("description=%S", meta.comment)
            .build()
        )
        .build()
        .also { staticOptionDtoBuilder.addProperty(it) }
    }

    return fileSpec
      .addType(staticOptionDtoBuilder.build())
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

    // Population is split into fixed-size chunks so no single generated method exceeds the
    // JVM 64KB bytecode limit regardless of how many enums are annotated.
    val fillChunks = metas.chunked(FILL_CHUNK_SIZE)
    val lazyBlock = CodeBlock.builder()
      .beginControlFlow("lazy")
      .addStatement("val dto = %T()", dtoType)
    fillChunks.forEachIndexed { index, _ ->
      lazyBlock.addStatement("fillStaticOptions%L(dto)", index)
    }
    lazyBlock.addStatement("dto")
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

    fillChunks.forEachIndexed { index, chunk ->
      val fillBuilder = FunSpec.builder("fillStaticOptions$index")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("dto", dtoType)
        .addStatement("val activeOverrides = zygardeApiProperties.staticOptionApi.active")
      chunk.forEach { meta ->
        fillBuilder.addStatement(
          "dto.%L = %T.values().map { it.toOptionDto(activeOverrides[%S]) }",
          meta.propertyName,
          meta.enumTypeName,
          meta.optionKey,
        )
      }
      staticOptionControllerBuilder.addFunction(fillBuilder.build())
    }

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

  companion object {
    // ~50 assignments per fill method keeps generated bytecode far below the 64KB method limit.
    private const val FILL_CHUNK_SIZE = 50
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
