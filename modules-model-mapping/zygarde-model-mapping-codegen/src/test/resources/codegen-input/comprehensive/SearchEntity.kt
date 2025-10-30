package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.RequestDto
import zygarde.codegen.SearchType
import zygarde.codegen.ZyModel

@ZyModel
data class SearchEntity(
  @ApiProp(
    comment = "ID",
    requestDto = [RequestDto(name = "SearchReq", searchType = SearchType.EQ)]
  )
  val id: Long,

  @ApiProp(
    comment = "Name",
    requestDto = [RequestDto(name = "SearchReq", searchType = SearchType.KEYWORD)]
  )
  val name: String,

  @ApiProp(
    comment = "Status",
    requestDto = [RequestDto(name = "SearchReq", searchType = SearchType.IN_LIST)]
  )
  val status: String,

  @ApiProp(
    comment = "Count",
    requestDto = [RequestDto(name = "SearchReq", searchType = SearchType.GT)]
  )
  val count: Int,

  @ApiProp(
    comment = "Price",
    requestDto = [RequestDto(name = "SearchReq", searchType = SearchType.LTE)]
  )
  val price: Double
)
