package zygarde.codegen

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AdditionalDtoProps(
  val props: Array<AdditionalDtoProp> = []
)
