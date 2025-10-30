package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.ZyModel

@ZyModel
data class ItemForCollection(
  @ApiProp(
    comment = "ID",
    dto = [Dto(name = "ItemDto", applyValueFromEntity = true)]
  )
  val id: Long
)

@ZyModel
data class EntityWithCollection(
  @ApiProp(
    comment = "ID",
    dto = [Dto(name = "CollectionDto", applyValueFromEntity = true)]
  )
  val id: Long,

  @ApiProp(
    comment = "Items",
    dto = [Dto(name = "CollectionDto", ref = "ItemDto", refCollection = true, applyValueFromEntity = true)]
  )
  val items: Collection<ItemForCollection>,

  @ApiProp(
    comment = "Nullable items",
    dto = [Dto(name = "CollectionDto", ref = "ItemDto", refCollection = true, refNullable = true, applyValueFromEntity = true)]
  )
  val nullableItems: Collection<ItemForCollection?>?
)
