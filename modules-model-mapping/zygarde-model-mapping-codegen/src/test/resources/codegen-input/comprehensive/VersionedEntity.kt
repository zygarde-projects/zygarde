package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.RequestDto
import zygarde.codegen.ZyModel

@ZyModel
data class VersionedEntity(
  @ApiProp(
    comment = "Field v1",
    requestDto = [RequestDto(name = "VersionedReq", applyValueToEntity = true, sinceApiVersion = 1)]
  )
  var fieldV1: String,

  @ApiProp(
    comment = "Field v2",
    requestDto = [RequestDto(name = "VersionedReq", applyValueToEntity = true, sinceApiVersion = 2)]
  )
  var fieldV2: String?
)
