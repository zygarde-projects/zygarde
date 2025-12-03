package zygarde.test.input

import zygarde.codegen.StaticOptionApi
import zygarde.data.option.OptionEnum

@StaticOptionApi(comment = "Test Status")
enum class TestStatus(
  override val label: String
) : OptionEnum {
  ACTIVE("Active"),
  INACTIVE("Inactive"),
}
