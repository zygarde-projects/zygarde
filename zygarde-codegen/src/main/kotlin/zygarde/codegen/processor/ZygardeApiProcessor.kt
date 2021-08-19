package zygarde.codegen.processor

import com.google.auto.service.AutoService
import zygarde.codegen.ZyApi
import zygarde.codegen.ZyModel
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.generator.impl.ZygardeApiGenerator
import zygarde.codegen.generator.impl.ZygardeApiPropGenerator
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

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
    ZygardeApiPropGenerator(processingEnv).generateModelForZyModelElements(elementsAnnotatedWithZyModel)
    ZygardeApiGenerator(processingEnv).generateApi(elementsAnnotatedWithZyApi)
    return false
  }
}
