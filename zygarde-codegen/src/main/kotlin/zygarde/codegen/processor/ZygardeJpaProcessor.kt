package zygarde.codegen.processor

import com.google.auto.service.AutoService
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZyModel
import zygarde.codegen.generator.impl.ZygardeJpaDaoGenerator
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.persistence.Entity

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(ZygardeKaptOptions.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ZygardeJpaProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(ZyModel::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elementsAnnotatedWithEntity = roundEnv.getElementsAnnotatedWith(Entity::class.java)
    ZygardeJpaDaoGenerator(processingEnv).generateDaoForEntityElements(elementsAnnotatedWithEntity)
    return false
  }
}
