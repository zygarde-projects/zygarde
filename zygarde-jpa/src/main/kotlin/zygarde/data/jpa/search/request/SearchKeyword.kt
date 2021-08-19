package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
open class SearchKeyword(
  @ApiModelProperty(notes = "關鍵字")
  var keyword: String? = null,
  @ApiModelProperty(notes = "關鍵字查詢類型")
  var type: SearchKeywordType = SearchKeywordType.STARTS_WITH
)
