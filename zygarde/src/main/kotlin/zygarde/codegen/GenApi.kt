package zygarde.codegen

import org.springframework.web.bind.annotation.RequestMethod
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenApi(
  val method: RequestMethod,
  val path: String,
  val pathVariable: Array<ApiPathVariable> = [],
  val api: String,
  val apiDescription: String = "",
  val service: String,
  val servicePostProcessing: Boolean = false,
  val reqRef: String = "",
  val reqRefClass: KClass<*> = Any::class,
  val reqCollection: Boolean = false,
  val resRef: String = "",
  val resRefClass: KClass<*> = Any::class,
  val resCollection: Boolean = false,
  val resPage: Boolean = false
)
