package zygarde.data.option

interface OptionEnum {
  val name: String
  val label: String
  val active: Boolean get() = true

  fun toOptionDto() = OptionDto(name, label, active)
}
