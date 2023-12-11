package zygarde.codegen.processor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.auto.service.AutoService
import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ZygardeApiGeneratorKaptOptions
import zygarde.codegen.ZygardeStaticOptionApiGeneratorTargetFolder
import zygarde.codegen.generator.impl.ZygardeStaticOptionApiGenerator
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
  ZygardeStaticOptionApiProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME,
  ZygardeApiGeneratorKaptOptions.STATIC_OPTION_API_CONFIG_JSON,
)
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
      ZygardeStaticOptionApiGenerator(
        processingEnv,
        processingEnv.options[ZygardeApiGeneratorKaptOptions.STATIC_OPTION_API_CONFIG_JSON]
          ?.let { File(it) }
          ?.takeIf { it.exists() }
          ?.let {
            val objectMapper = jacksonObjectMapper()
            objectMapper.readValue(
              it,
              ZygardeStaticOptionApiGeneratorTargetFolder::class.java
            )
          }

      )
        .generateStaticOptionApi(elements)
    }
    return false
  }
}
