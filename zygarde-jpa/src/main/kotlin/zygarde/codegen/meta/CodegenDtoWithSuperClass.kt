package zygarde.codegen.meta

import kotlin.reflect.KClass

interface CodegenDtoWithSuperClass : CodegenDto {
  override val name: String
  val superClass: KClass<*>
  override fun superClass(): KClass<*>? = superClass
  override fun superClassRef(): String? = null
}
