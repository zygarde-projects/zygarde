package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@ApiModel
open class PagingAndSortingRequest {
  @ApiModelProperty(notes = "分頁", required = true)
  var paging: PagingRequest = PagingRequest()

  @ApiModelProperty(notes = "排序", required = false)
  var sorting: SortingRequest? = null

  @ApiModelProperty(notes = "排序(new)", required = false)
  var sorts: List<SortField> = emptyList()

  fun toPageRequest(): PageRequest {
    return PageRequest.of(
      paging.page - 1,
      paging.pageSize,
      buildSorts()
    )
  }

  private fun buildSorts(): Sort {
    val allSorts = listOf(
      this.sorting?.let { sorting -> sorting.sortFields.mapNotNull { sf -> SortField(sorting.sort, sf).toSort() } } ?: emptyList(),
      this.sorts.mapNotNull { it.toSort() }
    ).flatten()

    return allSorts
      .takeIf { it.isNotEmpty() }
      ?.reduceRight { s1, s2 -> s1.and(s2) }
      ?: Sort.unsorted()
  }
}
