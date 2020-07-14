package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
open class PagingAndSortingRequest {
  @ApiModelProperty(notes = "分頁")
  var paging: PagingRequest = PagingRequest()
  @ApiModelProperty(notes = "排序")
  var sorting: SortingRequest? = null
}
