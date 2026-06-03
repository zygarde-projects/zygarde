package zygarde.api.exception

import jakarta.servlet.http.HttpServletRequest
import zygarde.core.exception.ErrorCode
import zygarde.data.api.ApiErrorResponse

fun interface ApiErrorResponseFactory {
  fun create(
    errorCode: ErrorCode,
    messages: List<String>,
    throwable: Throwable?,
    request: HttpServletRequest?
  ): Any
}

class DefaultApiErrorResponseFactory : ApiErrorResponseFactory {
  override fun create(
    errorCode: ErrorCode,
    messages: List<String>,
    throwable: Throwable?,
    request: HttpServletRequest?
  ): Any = ApiErrorResponse(errorCode, messages)
}
