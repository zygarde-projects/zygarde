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

@StaticOptionApi(comment = "Todo Priority", key = "todo-priority", path = "priorities")
enum class PriorityLevel(
  override val label: String
) : OptionEnum {
  LOW("Low"),
  HIGH("High"),
}
