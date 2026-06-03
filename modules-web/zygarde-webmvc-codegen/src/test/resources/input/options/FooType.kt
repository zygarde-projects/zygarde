package zygarde.options

import zygarde.codegen.StaticOptionApi
import zygarde.data.option.OptionEnum

@StaticOptionApi(comment = "foo bar", key = "foo-type", path = "foo-types")
enum class FooType(
  override val label: String
): OptionEnum {
  FOO("foo"),
  BAR("bar")
}
