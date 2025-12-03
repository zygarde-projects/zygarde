package zygarde.codegen.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import zygarde.codegen.ZyModel
import zygarde.codegen.ksp.generator.ZygardeApiPropKspGenerator

class ZygardeModelMappingKspProcessor(
  private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
  private val codeGenerator: CodeGenerator = environment.codeGenerator
  private val logger: KSPLogger = environment.logger
  private val options: Map<String, String> = environment.options

  private var invoked = false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) {
      return emptyList()
    }
    invoked = true

    val zyModelAnnotationName = ZyModel::class.qualifiedName ?: return emptyList()

    val zyModelSymbols = resolver.getSymbolsWithAnnotation(zyModelAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .toList()

    if (zyModelSymbols.isEmpty()) {
      return emptyList()
    }

    ZygardeApiPropKspGenerator(codeGenerator, logger, options)
      .generateModelForZyModelElements(zyModelSymbols)

    return emptyList()
  }
}
