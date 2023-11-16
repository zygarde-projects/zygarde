package zygarde.codegen.generator

import com.squareup.kotlinpoet.TypeName
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZygardeKaptOptions.Companion.BASE_PACKAGE
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allFieldsIncludeSuper
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allSuperTypes
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.nullableTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.resolveGenericFieldTypeMap
import zygarde.codegen.extension.kotlinpoet.kotlinTypeName
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypeException

abstract class AbstractZygardeGenerator(
  val processingEnv: ProcessingEnvironment,
) {

  fun packageName(pack: String) = processingEnv.options.getOrDefault(BASE_PACKAGE, "zygarde.generated") + ".$pack"

  fun safeGetTypeFromAnnotation(block: () -> TypeName): TypeName {
    return try {
      block.invoke()
    } catch (e: MirroredTypeException) {
      e.typeMirror.kotlinTypeName(false)
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

  protected fun Element.resolveFieldType(fieldElement: Element): TypeName {
    val genericFieldTypeMap = this.resolveGenericFieldTypeMap(processingEnv)
    val fieldTypeMirror = fieldElement.asType()
    val fieldLocation = "${fieldElement.enclosingElement}_$fieldTypeMirror"
    return genericFieldTypeMap.getOrElse(fieldLocation) {
      fieldElement.nullableTypeName()
    }
  }
}
