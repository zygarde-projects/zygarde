package zygarde.codegen.processor

import com.google.auto.service.AutoService
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZyApi
import zygarde.codegen.ZyModel
import zygarde.codegen.generator.impl.ZygardeApiGenerator
import zygarde.codegen.generator.impl.ZygardeApiPropGenerator
import zygarde.codegen.generator.impl.ZygardeEntitySearchFieldGenerator
import javax.annotation.processing.*
import javax.persistence.Entity

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(ZygardeKaptOptions.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ZygardeApiProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(ZyModel::class.java.name, ZyApi::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elementsAnnotatedWithZyModel = roundEnv.getElementsAnnotatedWith(ZyModel::class.java)
    val elementsAnnotatedWithZyApi = roundEnv.getElementsAnnotatedWith(ZyApi::class.java)
    val elementsAnnotatedWithEntity = roundEnv.getElementsAnnotatedWith(Entity::class.java)
    ZygardeEntitySearchFieldGenerator(processingEnv).generateSearchFieldForEntityElements(elementsAnnotatedWithEntity)
    ZygardeApiPropGenerator(processingEnv).generateModelForZyModelElements(elementsAnnotatedWithZyModel)
    ZygardeApiGenerator(processingEnv).generateApi(elementsAnnotatedWithZyApi)
    return false
  }
}
