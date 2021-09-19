package zygarde.test.input

import zygarde.codegen.meta.ZyModelMeta

@ZyModelMeta
data class Item(
  var id: String,
  var price: Double?,
  var amount: Int,
)
