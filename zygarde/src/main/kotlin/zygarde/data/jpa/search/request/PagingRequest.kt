package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * @author leo
 */
@ApiModel
data class PagingRequest(
  @ApiModelProperty(notes = "頁次（從1開始）")
  var page: Int = 1,
  @ApiModelProperty(notes = "每頁數量")
  var pageSize: Int = 10
)

fun PagingRequest.toPageRequest(sort: Sort): PageRequest {
  return PageRequest.of(
    page - 1,
    pageSize,
    sort
  )
}
