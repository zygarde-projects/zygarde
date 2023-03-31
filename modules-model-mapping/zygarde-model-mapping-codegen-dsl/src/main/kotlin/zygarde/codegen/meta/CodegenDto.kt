package zygarde.codegen.meta

import com.squareup.kotlinpoet.AnnotationSpec
import kotlin.reflect.KClass

interface CodegenDto {
  val name: String
  fun superClass(): KClass<*>?
  fun superClassRef(): String?
  fun superInterfaces(): List<KClass<*>> = emptyList()
  fun annotations(): List<AnnotationSpec> = emptyList()
}
