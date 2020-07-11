package zygarde.data.api

import io.swagger.annotations.ApiModel
import java.time.LocalDate
import zygarde.data.jpa.search.request.SearchDateRange

@ApiModel
class ApiSearchDateRange(
  from: LocalDate? = null,
  to: LocalDate? = null
) : SearchDateRange(from, to)
