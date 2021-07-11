package zygarde.codegen.meta

import kotlin.reflect.KClass

interface CodegenDtoSimple : CodegenDto {
  override val name: String
  override fun superClass(): KClass<*>? = null
  override fun superClassRef(): String? = null
}
