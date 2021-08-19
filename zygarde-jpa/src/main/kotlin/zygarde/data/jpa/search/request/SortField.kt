package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.domain.Sort

@ApiModel
data class SortField(
  @ApiModelProperty(notes = "排序方式")
  var sort: Sort.Direction? = null,
  @ApiModelProperty(notes = "排序欄位")
  var field: String? = null
) {
  fun toSort(): Sort? {
    return sort?.let { s -> field?.let { f -> Sort.by(s, f) } }
  }
}
