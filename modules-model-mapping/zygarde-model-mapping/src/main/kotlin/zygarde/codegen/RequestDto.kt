package zygarde.codegen

import kotlin.reflect.KClass
import zygarde.codegen.value.NoOpValueProvider
import zygarde.codegen.value.ValueProvider

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RequestDto(
  val name: String = "",
  val names: Array<String> = [],
  /**
   * override fieldName in DTO
   */
  val fieldName: String = "",
  val refClass: KClass<*> = Any::class,
  val refClassNullable: Boolean = false,
  val refCollection: Boolean = false,
  val ref: String = "",
  val refNullable: Boolean = false,
  val applyValueToEntity: Boolean = true,
  val valueProvider: KClass<out ValueProvider<*, *>> = NoOpValueProvider::class,
  val searchType: SearchType = SearchType.NONE,
  val searchForField: String = "",
  val notNullInReq: Boolean = false,
  val forceNullableInReq: Boolean = false,
)
