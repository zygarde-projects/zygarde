package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.RequestDto
import zygarde.codegen.ZyModel

@ZyModel
data class RequestEntity(
  @ApiProp(
    comment = "Title",
    requestDto = [RequestDto(name = "RequestEntityReq", applyValueToEntity = true)]
  )
  var title: String,

  @ApiProp(
    comment = "Status",
    requestDto = [RequestDto(name = "RequestEntityReq", applyValueToEntity = true)]
  )
  var status: String?
)
