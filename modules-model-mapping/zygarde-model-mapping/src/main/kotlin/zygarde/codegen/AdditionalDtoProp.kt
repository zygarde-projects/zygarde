package zygarde.codegen

import kotlin.reflect.KClass
import zygarde.codegen.value.NoOpValueProvider
import zygarde.codegen.value.ValueProvider

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AdditionalDtoProp(
  val forDto: Array<String> = [],
  val field: String = "",
  val fieldType: KClass<*> = Any::class,
  val comment: String = "",
  val valueProvider: KClass<out ValueProvider<*, *>> = NoOpValueProvider::class,
  val entityValueProvider: KClass<out ValueProvider<*, *>> = NoOpValueProvider::class
)
