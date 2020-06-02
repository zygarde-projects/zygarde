package zygarde.data.jpa.search.request

import java.time.LocalDate

/**
 * @author leo
 */
open class SearchDateRange(
  var from: LocalDate? = null,
  var to: LocalDate? = null
)
