package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema
import zygarde.data.jpa.search.request.SearchDateRange
import java.time.LocalDate

@Schema
class ApiSearchDateRange(
  from: LocalDate? = null,
  to: LocalDate? = null
) : SearchDateRange(from, to)
