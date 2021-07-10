package zygarde.codegen.meta

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterDto(
  vararg val dtos: String,
  val superClassRef: String = "",
  val superClass: KClass<*> = Any::class,
)
