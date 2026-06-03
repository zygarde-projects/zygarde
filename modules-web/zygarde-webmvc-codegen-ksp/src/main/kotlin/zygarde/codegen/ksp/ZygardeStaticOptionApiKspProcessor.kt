package zygarde.codegen.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ksp.ZygardeWebMvcKspOptions.API_STATIC_OPTION_PACKAGE
import zygarde.codegen.ksp.ZygardeWebMvcKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.extension.getArgumentValueAsString
import zygarde.codegen.ksp.extension.implementsInterface
import zygarde.codegen.ksp.extension.name
import zygarde.codegen.generator.StaticOptionApiGeneratorSupport
import zygarde.codegen.generator.StaticOptionApiMeta
import zygarde.data.option.OptionEnum

class ZygardeStaticOptionApiKspProcessor(
  private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
  private val codeGenerator: CodeGenerator = environment.codeGenerator
  private val options: Map<String, String> = environment.options

  private var invoked = false

  private fun packageName(pack: String): String {
    val basePackage = options.getOrDefault(BASE_PACKAGE, "zygarde.generated")
    return "$basePackage.$pack"
  }

  private val optionPackage: String by lazy {
    packageName(options.getOrDefault(API_STATIC_OPTION_PACKAGE, "api.option"))
  }

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

    val metas = elements.map { it.toStaticOptionApiMeta() }
    val generator = StaticOptionApiGeneratorSupport(optionPackage)

    generator.generateStaticOptionDto(metas).writeTo(codeGenerator, aggregating = true)
    generator.generateStaticOptionApiInterface(metas).writeTo(codeGenerator, aggregating = true)
    generator.generateStaticOptionController(metas).writeTo(codeGenerator, aggregating = true)

    return emptyList()
  }

  private fun KSClassDeclaration.toStaticOptionApiMeta(): StaticOptionApiMeta {
    val staticOptionApiAnn = annotations.find { it.shortName.asString() == "StaticOptionApi" }
    return StaticOptionApiMeta(
      enumName = name(),
      enumTypeName = asType(emptyList()).toTypeName(),
      comment = staticOptionApiAnn?.getArgumentValueAsString("comment") ?: "",
      key = staticOptionApiAnn?.getArgumentValueAsString("key") ?: "",
      path = staticOptionApiAnn?.getArgumentValueAsString("path") ?: "",
      sourceName = qualifiedName?.asString() ?: name(),
    )
  }
}
