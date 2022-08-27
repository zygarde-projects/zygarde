package zygarde.codegen

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ApiProp(
  val comment: String = "",
  val dto: Array<Dto> = [],
  val requestDto: Array<RequestDto> = []
)
