package zygarde.codegen.processor

import com.google.auto.service.AutoService
import zygarde.codegen.ZyModel
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.generator.impl.ZygardeEntityFieldGenerator
import zygarde.codegen.generator.impl.ZygardeJpaDaoGenerator
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.persistence.Entity

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
  "kapt.kotlin.generated",
  ZygardeKaptOptions.BASE_PACKAGE,
  ZygardeKaptOptions.DAO_SUFFIX,
  ZygardeKaptOptions.DAO_PACKAGE,
  ZygardeKaptOptions.DAO_ENHANCED_IMPL,
  ZygardeKaptOptions.DAO_COMBINE,
)
class ZygardeJpaProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(ZyModel::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elementsAnnotatedWithEntity = roundEnv.getElementsAnnotatedWith(Entity::class.java)
    val elementsAnnotatedWithZyModelEntity = roundEnv.getElementsAnnotatedWith(ZyModel::class.java)
      .filter { elementsAnnotatedWithEntity.contains(it) }
    ZygardeEntityFieldGenerator(processingEnv).generateSearchFieldForEntityElements(elementsAnnotatedWithZyModelEntity)
    ZygardeJpaDaoGenerator(processingEnv).generateDaoForEntityElements(elementsAnnotatedWithZyModelEntity)
    return false
  }
}
