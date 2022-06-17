package zygarde.test.input

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.ZyModel
import zygarde.codegen.meta.ZyModelMeta

@ZyModel
@ZyModelMeta
data class Item(
  var id: String,
  var price: Double?,
  var amount: Int,
  @ApiProp(
    dto = [Dto("itemDto", refClass = String::class, refCollection = true, refClassNullable = true)]
  )
  var tagList: Collection<String?>,
)
