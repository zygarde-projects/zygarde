package zygarde.codegen.processor

import com.google.auto.service.AutoService
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.generator.impl.ZygardeRegisterDtosGenerator
import zygarde.codegen.meta.RegisterDtos
import javax.annotation.processing.*

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(ZygardeKaptOptions.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ZygardeRegisterDtosProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(RegisterDtos::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elementsAnnotatedWithRegisterDtos = roundEnv.getElementsAnnotatedWith(RegisterDtos::class.java)
    ZygardeRegisterDtosGenerator(processingEnv).generateDtos(elementsAnnotatedWithRegisterDtos)
    return false
  }
}
