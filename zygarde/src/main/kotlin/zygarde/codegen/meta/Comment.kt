package zygarde.codegen.meta

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Comment(
  val comment: String = ""
)
