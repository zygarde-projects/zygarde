package zygarde.test.input

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.RequestDto
import zygarde.codegen.ZyModel

@ZyModel
data class Item(
  var id: String,
  var price: Double?,
  @ApiProp(
    comment = "item amount",
    dto = [Dto("ItemDto")],
    requestDto = [RequestDto("CreateItemReq")]
  )
  var amount: Int,
  @ApiProp(
    comment = "tags",
    dto = [Dto("ItemDto", refClass = String::class, refCollection = true)]
  )
  var tagList: Collection<String?>,
)
