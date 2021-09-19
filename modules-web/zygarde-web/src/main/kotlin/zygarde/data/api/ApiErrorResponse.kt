package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema
import zygarde.core.exception.ErrorCode

/**
 * @author leo
 */
@Schema
class ApiErrorResponse(
  val code: String,
  val name: String,
  val messages: List<String>
) {
  constructor(errorCode: ErrorCode, messages: List<String>) : this(
    errorCode.code,
    errorCode.name,
    messages
  )
}
