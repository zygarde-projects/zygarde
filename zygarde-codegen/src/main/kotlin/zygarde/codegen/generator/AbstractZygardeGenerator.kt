package zygarde.codegen.generator

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZygardeKaptOptions.Companion.BASE_PACKAGE
import zygarde.codegen.extension.kotlinpoet.allFieldsIncludeSuper
import zygarde.data.option.OptionEnum
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

abstract class AbstractZygardeGenerator(
  val processingEnv: ProcessingEnvironment
) {

  protected val fileTarget: File by lazy {
    File("${processingEnv.options[ZygardeKaptOptions.KAPT_KOTLIN_GENERATED_OPTION_NAME]}")
  }

  fun packageName(pack: String) = processingEnv.options.getOrDefault(BASE_PACKAGE, "zygarde.generated") + ".$pack"

  fun safeGetTypeFromAnnotation(block: () -> TypeName): TypeName {
    try {
      return block.invoke()
    } catch (e: MirroredTypeException) {
      return e.typeMirror.asTypeName()
    }
  }

  protected inline fun <reified T : Any> erasureType(): TypeMirror {
    return processingEnv.typeUtils.erasure(
      processingEnv.elementUtils.getTypeElement(T::class.qualifiedName).asType()
    )
  }

  protected inline fun <reified T : Any> Element.isTypeOf(): Boolean {
    return processingEnv.typeUtils.isAssignable(this.asType(), erasureType<T>())
  }

  protected fun Element.allFieldsIncludeSuper(): List<Element> {
    return allFieldsIncludeSuper(processingEnv)
  }

  protected fun KClass<*>.generic(vararg typeName: TypeName): TypeName {
    return asClassName().parameterizedBy(
      *typeName
    )
  }
}
