package zygarde.codegen.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import zygarde.codegen.ZyModel
import zygarde.codegen.ksp.generator.ZygardeEntityFieldKspGenerator
import zygarde.codegen.ksp.generator.ZygardeJpaDaoExtensionKspGenerator
import zygarde.codegen.ksp.generator.ZygardeJpaDaoKspGenerator

class ZygardeJpaKspProcessor(
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
    val entityAnnotationName = "javax.persistence.Entity"
    val embeddableAnnotationName = "javax.persistence.Embeddable"
    val mappedSuperclassAnnotationName = "javax.persistence.MappedSuperclass"

    val zyModelSymbols = resolver.getSymbolsWithAnnotation(zyModelAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .toList()

    val entitySymbols = resolver.getSymbolsWithAnnotation(entityAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .toSet()

    val embeddableSymbols = resolver.getSymbolsWithAnnotation(embeddableAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .toSet()

    val mappedSuperclassSymbols = resolver.getSymbolsWithAnnotation(mappedSuperclassAnnotationName)
      .filterIsInstance<KSClassDeclaration>()
      .toSet()

    // Generate search fields for entities, embeddables, and mapped superclasses with @ZyModel
    val elementsForSearchField = zyModelSymbols.filter {
      entitySymbols.contains(it) || embeddableSymbols.contains(it) || mappedSuperclassSymbols.contains(it)
    }
    ZygardeEntityFieldKspGenerator(codeGenerator, logger, options)
      .generateSearchFieldForEntityElements(elementsForSearchField)

    // Generate DAOs for entities with @ZyModel
    val elementsForDao = zyModelSymbols.filter { entitySymbols.contains(it) }
    ZygardeJpaDaoKspGenerator(codeGenerator, logger, options)
      .generateDaoForEntityElements(elementsForDao)
    ZygardeJpaDaoExtensionKspGenerator(codeGenerator, logger, options)
      .generateDaoExtensionsForEntityElements(elementsForDao)

    return emptyList()
  }
}
