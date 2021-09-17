package zygarde.codegen

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ZyApi(
  val api: Array<GenApi> = []
)
