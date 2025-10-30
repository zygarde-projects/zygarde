package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.ZyModel

@ZyModel
data class SimpleEntity(
  @ApiProp(
    comment = "Entity ID",
    dto = [Dto(name = "SimpleDto", applyValueFromEntity = true)]
  )
  val id: Long,

  @ApiProp(
    comment = "Name",
    dto = [Dto(name = "SimpleDto", applyValueFromEntity = true)]
  )
  val name: String,

  @ApiProp(
    comment = "Description",
    dto = [Dto(name = "SimpleDto", applyValueFromEntity = true)]
  )
  val description: String?
)
