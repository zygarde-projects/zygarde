package zygarde.codegen.generator

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZygardeKaptOptions.Companion.BASE_PACKAGE
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allFieldsIncludeSuper
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allSuperTypes
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypeException

abstract class AbstractZygardeGenerator(
  val processingEnv: ProcessingEnvironment
) {

  fun packageName(pack: String) = processingEnv.options.getOrDefault(BASE_PACKAGE, "zygarde.generated") + ".$pack"

  fun safeGetTypeFromAnnotation(block: () -> TypeName): TypeName {
    return try {
      block.invoke()
    } catch (e: MirroredTypeException) {
      e.typeMirror.asTypeName()
    }
  }

  protected inline fun <reified T : Any> Element.isTypeOf(): Boolean {
    val qualifiedName = T::class.qualifiedName
    return this.allSuperTypes(processingEnv).map { it.toString() }.contains(qualifiedName)
  }

  protected fun Element.allFieldsIncludeSuper(): List<Element> {
    return allFieldsIncludeSuper(processingEnv)
  }

  protected fun folderToGenerate(
    kaptOptionForFolderToGenerate: String = ZygardeKaptOptions.KAPT_KOTLIN_GENERATED_OPTION_NAME
  ): File {
    return File("${processingEnv.options[kaptOptionForFolderToGenerate]}").also { it.mkdirs() }
  }
}
