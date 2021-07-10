package zygarde.codegen.meta

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterDtos(
  val group: String = "",
  vararg val values: RegisterDto,
)
