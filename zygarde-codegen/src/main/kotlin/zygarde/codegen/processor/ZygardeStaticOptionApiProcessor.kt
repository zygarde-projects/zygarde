package zygarde.codegen.processor

import com.google.auto.service.AutoService
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.generator.impl.ZygardeStaticOptionApiGenerator
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(ZygardeStaticOptionApiProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ZygardeStaticOptionApiProcessor : AbstractProcessor() {

  companion object {
    const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
  }

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(StaticOptionApi::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elements = roundEnv.getElementsAnnotatedWith(StaticOptionApi::class.java)
    if (elements.isNotEmpty()) {
      ZygardeStaticOptionApiGenerator(processingEnv).generateStaticOptionApi(elements)
    }
    return false
  }
}
