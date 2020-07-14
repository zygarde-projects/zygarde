package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.domain.Sort

/**
 * @author leo
 */
@ApiModel
data class SortingRequest(
  @ApiModelProperty(notes = "排序方式")
  var sort: Sort.Direction = Sort.Direction.DESC,
  @ApiModelProperty(notes = "排序欄位")
  var sortFields: List<String> = emptyList()
)

fun SortingRequest?.asSort(): Sort {
  if (this != null && sortFields.isNotEmpty()) {
    return Sort.by(sort, *sortFields.toTypedArray())
  }
  return Sort.unsorted()
}
