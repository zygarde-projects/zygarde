package zygarde.codegen.meta

@Target(AnnotationTarget.FIELD)
annotation class Comment(
  val comment: String = ""
)
