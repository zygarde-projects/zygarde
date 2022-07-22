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
import javax.persistence.Embeddable
import javax.persistence.Entity
import javax.persistence.MappedSuperclass

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
    val elementsAnnotatedWithEmbeddable = roundEnv.getElementsAnnotatedWith(Embeddable::class.java)
    val elementsAnnotatedWithMappedSuperclass = roundEnv.getElementsAnnotatedWith(MappedSuperclass::class.java)
    val elementsAnnotatedWithZyModel = roundEnv.getElementsAnnotatedWith(ZyModel::class.java)
    ZygardeEntityFieldGenerator(processingEnv).generateSearchFieldForEntityElements(
      elementsAnnotatedWithZyModel.filter {
        elementsAnnotatedWithEntity.contains(it) ||
          elementsAnnotatedWithEmbeddable.contains(it) ||
          elementsAnnotatedWithMappedSuperclass.contains(it)
      }
    )
    ZygardeJpaDaoGenerator(processingEnv).generateDaoForEntityElements(
      elementsAnnotatedWithZyModel.filter { elementsAnnotatedWithEntity.contains(it) }
    )
    return false
  }
}
