package zygarde.codegen

import org.springframework.web.bind.annotation.RequestMethod

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenApi(
  val method: RequestMethod,
  val path: String,
  val pathVariable: Array<ApiPathVariable> = [],
  val api: String,
  val apiDescription: String = "",
  val service: String,
  val reqRef: String,
  val reqCollection: Boolean = false,
  val resRef: String,
  val resCollection: Boolean = false,
  val resPage: Boolean = false
)
