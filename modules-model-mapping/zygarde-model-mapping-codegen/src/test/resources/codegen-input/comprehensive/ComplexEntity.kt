package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.RequestDto
import zygarde.codegen.SearchType
import zygarde.codegen.ZyModel

@ZyModel
data class ComplexEntity(
  @ApiProp(
    comment = "ID",
    dto = [Dto(name = "ComplexDto", applyValueFromEntity = true)],
    requestDto = [RequestDto(name = "ComplexReq", applyValueToEntity = true), RequestDto(name = "ComplexSearch", searchType = SearchType.EQ)]
  )
  var id: Long,

  @ApiProp(
    comment = "Name",
    dto = [Dto(name = "ComplexDto", applyValueFromEntity = true)],
    requestDto = [RequestDto(name = "ComplexReq", applyValueToEntity = true), RequestDto(name = "ComplexSearch", searchType = SearchType.KEYWORD)]
  )
  var name: String,

  @ApiProp(
    comment = "Status",
    dto = [Dto(name = "ComplexDto", fieldName = "currentStatus", applyValueFromEntity = true)],
    requestDto = [RequestDto(name = "ComplexReq", fieldName = "status", applyValueToEntity = true), RequestDto(name = "ComplexSearch", searchType = SearchType.IN_LIST)]
  )
  var status: String?,

  @ApiProp(
    comment = "Count",
    requestDto = [RequestDto(name = "ComplexSearch", searchType = SearchType.GTE)]
  )
  val count: Int,

  @ApiProp(
    comment = "Price",
    requestDto = [RequestDto(name = "ComplexSearch", searchType = SearchType.LTE)]
  )
  val price: Double
)
