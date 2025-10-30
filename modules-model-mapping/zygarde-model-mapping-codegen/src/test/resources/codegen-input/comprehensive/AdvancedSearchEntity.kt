package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.RequestDto
import zygarde.codegen.SearchType
import zygarde.codegen.ZyModel

@ZyModel
data class AdvancedSearchEntity(
  @ApiProp(
    comment = "NOT_EQ field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.NOT_EQ)]
  )
  val notEqField: Long,

  @ApiProp(
    comment = "LT field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.LT)]
  )
  val ltField: Int,

  @ApiProp(
    comment = "GT field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.GT)]
  )
  val gtField: Int,

  @ApiProp(
    comment = "STARTS_WITH field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.STARTS_WITH)]
  )
  val startsWithField: String,

  @ApiProp(
    comment = "ENDS_WITH field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.ENDS_WITH)]
  )
  val endsWithField: String,

  @ApiProp(
    comment = "CONTAINS field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.CONTAINS)]
  )
  val containsField: String,

  @ApiProp(
    comment = "LIST_CONTAINS_ANY field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.LIST_CONTAINS_ANY)]
  )
  val listContainsAnyField: String,

  @ApiProp(
    comment = "DATE_RANGE field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.DATE_RANGE)]
  )
  val dateRangeField: String,

  @ApiProp(
    comment = "DATE_TIME_RANGE field",
    requestDto = [RequestDto(name = "AdvancedSearch", searchType = SearchType.DATE_TIME_RANGE)]
  )
  val dateTimeRangeField: String
)
