package zygarde.codegen.processor

import com.google.auto.service.AutoService
import zygarde.codegen.ZyApi
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.generator.impl.ZygardeApiGenerator
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
    return mutableSetOf(ZyApi::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elementsAnnotatedWithZyApi = roundEnv.getElementsAnnotatedWith(ZyApi::class.java)
    ZygardeApiGenerator(processingEnv).generateApi(elementsAnnotatedWithZyApi)
    return false
  }
}
