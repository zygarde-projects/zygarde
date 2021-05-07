package zygarde.codegen.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.model.EntityMeta
import java.io.File
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
@SupportedOptions(ZygardeKaptOptions.META_GEN_TARGET)
class EntityMetaGenerateProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(Entity::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elementsAnnotatedWithEntity = roundEnv.getElementsAnnotatedWith(Entity::class.java)
    if (elementsAnnotatedWithEntity.isEmpty()) {
      return false
    }

    val fileTarget = File(
      "${processingEnv.options.getOrDefault(ZygardeKaptOptions.META_GEN_TARGET, "buildSrc/src")}"
    )

    val objectBuilder = TypeSpec.objectBuilder("EntityMetas")

    elementsAnnotatedWithEntity.forEach { entityElement ->

      objectBuilder.addProperty(
        PropertySpec
          .builder(
            entityElement.simpleName.toString(),
            EntityMeta::class,
            KModifier.PUBLIC,
          )
          .mutable(false)
          .initializer(
            CodeBlock.builder()
              .addStatement("""%T("$entityElement")""", EntityMeta::class.asClassName())
              .build()
          )
          .build()
      )
    }

    val fileSpec = FileSpec.builder("zygarde.codegen", "EntityMetas")
      .addType(
        objectBuilder.build()
      )
    fileSpec
      .build()
      .writeTo(fileTarget)

    return false
  }
}
