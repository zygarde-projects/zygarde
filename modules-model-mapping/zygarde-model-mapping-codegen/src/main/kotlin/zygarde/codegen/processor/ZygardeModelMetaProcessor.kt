package zygarde.codegen.processor

import com.google.auto.service.AutoService
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.generator.impl.ZygardeModelMetaGenerator
import zygarde.codegen.meta.ZyModelMeta
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
  ZygardeKaptOptions.MODEL_META_GENERATE_TARGET,
  ZygardeKaptOptions.MODEL_META_GENERATE_PACKAGE,
)
class ZygardeModelMetaProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(ZyModelMeta::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elements = roundEnv.getElementsAnnotatedWith(ZyModelMeta::class.java)
    ZygardeModelMetaGenerator(processingEnv).generateModelMeta(elements)
    return false
  }
}
