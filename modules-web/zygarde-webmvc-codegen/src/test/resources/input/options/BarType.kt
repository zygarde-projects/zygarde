package codegen.options

import zygarde.codegen.StaticOptionApi
import zygarde.data.option.OptionEnum

@StaticOptionApi(comment = "bar bar")
enum class BarType(
  override val label: String
): OptionEnum {
  LOL("lol"),
  QAQ("qaq")
}
