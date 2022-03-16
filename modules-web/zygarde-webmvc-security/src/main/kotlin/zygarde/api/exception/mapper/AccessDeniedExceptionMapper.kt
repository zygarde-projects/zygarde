package zygarde.api.exception.mapper

import zygarde.core.exception.ApiErrorCode
import zygarde.api.exception.ExceptionToBusinessExceptionMapper
import zygarde.core.exception.BusinessException

typealias AccessDenied = org.springframework.security.access.AccessDeniedException

class AccessDeniedExceptionMapper : ExceptionToBusinessExceptionMapper<AccessDenied>() {
  override fun supported(t: Throwable): Boolean = t is AccessDenied

  override fun transform(t: AccessDenied): BusinessException {
    return BusinessException(ApiErrorCode.UNAUTHORIZED, t.message ?: t.toString())
  }
}
