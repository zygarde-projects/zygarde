package zygarde.codegen

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiPathVariable(
  val value: String = "",
  val type: KClass<*> = Any::class
)
