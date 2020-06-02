package zygarde.data.jpa.search.request

import java.time.LocalDateTime

/**
 * @author leo
 */
open class SearchDateTimeRange(
  var from: LocalDateTime? = null,
  var until: LocalDateTime? = null
)
