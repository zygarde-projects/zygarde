package zygarde.codegen

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DtoInherit(
  val dto: String,
  val inherit: KClass<*>
)
