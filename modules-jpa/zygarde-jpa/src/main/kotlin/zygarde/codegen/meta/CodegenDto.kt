package zygarde.codegen.meta

import kotlin.reflect.KClass

interface CodegenDto {
  val name: String
  fun superClass(): KClass<*>?
  fun superClassRef(): String?
}
