package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema
import zygarde.data.search.SearchDateRange
import java.time.LocalDate

@Schema
class ApiSearchDateRange(
  from: LocalDate? = null,
  to: LocalDate? = null
) : SearchDateRange(from, to)
