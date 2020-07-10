package puni.options

import zygarde.codegen.StaticOptionApi
import zygarde.data.option.OptionEnum

@StaticOptionApi(comment = "foo bar")
enum class FooType(
  override val label: String
): OptionEnum {
  FOO("foo"),
  BAR("bar")
}
