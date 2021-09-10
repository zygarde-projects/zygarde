package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema
import zygarde.data.jpa.search.request.SearchDateTimeRange
import java.time.LocalDateTime

@Schema
class ApiSearchDateTimeRange(
  from: LocalDateTime? = null,
  until: LocalDateTime? = null
) : SearchDateTimeRange(from, until)
