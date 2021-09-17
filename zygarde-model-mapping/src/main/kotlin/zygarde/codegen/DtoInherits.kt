package zygarde.codegen

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DtoInherits(
  val value: Array<DtoInherit> = []
)
