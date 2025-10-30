package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.ZyModel

@ZyModel
data class RelatedEntityA(
  @ApiProp(
    comment = "ID",
    dto = [Dto(name = "RelatedADto", applyValueFromEntity = true)]
  )
  val id: Long,

  @ApiProp(
    comment = "Name",
    dto = [Dto(name = "RelatedADto", applyValueFromEntity = true)]
  )
  val name: String
)

@ZyModel
data class EntityWithRef(
  @ApiProp(
    comment = "ID",
    dto = [Dto(name = "EntityWithRefDto", applyValueFromEntity = true)]
  )
  val id: Long,

  @ApiProp(
    comment = "Related entity",
    dto = [Dto(name = "EntityWithRefDto", ref = "RelatedADto", applyValueFromEntity = true)]
  )
  val related: RelatedEntityA,

  @ApiProp(
    comment = "Nullable related entity",
    dto = [Dto(name = "EntityWithRefDto", ref = "RelatedADto", refNullable = true, applyValueFromEntity = true)]
  )
  val nullableRelated: RelatedEntityA?
)
