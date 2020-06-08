package zygarde.data.api

import io.swagger.annotations.ApiModel
import zygarde.core.exception.ErrorCode

/**
 * @author leo
 */
@ApiModel
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
