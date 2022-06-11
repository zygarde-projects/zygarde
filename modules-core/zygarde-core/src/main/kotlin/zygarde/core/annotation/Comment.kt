package zygarde.core.annotation

@Target(AnnotationTarget.FIELD)
annotation class Comment(
  val comment: String = ""
)
