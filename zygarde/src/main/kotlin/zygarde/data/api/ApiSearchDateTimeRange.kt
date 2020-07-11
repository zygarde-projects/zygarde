package zygarde.data.api

import io.swagger.annotations.ApiModel
import java.time.LocalDateTime
import zygarde.data.jpa.search.request.SearchDateTimeRange

@ApiModel
class ApiSearchDateTimeRange(
  from: LocalDateTime? = null,
  until: LocalDateTime? = null
) : SearchDateTimeRange(from, until)
