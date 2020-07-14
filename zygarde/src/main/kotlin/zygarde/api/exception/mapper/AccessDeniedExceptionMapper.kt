package zygarde.api.exception.mapper

import org.springframework.stereotype.Component
import zygarde.api.exception.ExceptionToBusinessExceptionMapper
import zygarde.api.ApiErrorCode
import zygarde.core.exception.BusinessException

typealias AccessDenied = org.springframework.security.access.AccessDeniedException

@Component
class AccessDeniedExceptionMapper : ExceptionToBusinessExceptionMapper<AccessDenied>() {
  override fun supported(t: Throwable): Boolean = t is AccessDenied

  override fun transform(t: AccessDenied): BusinessException {
    return BusinessException(ApiErrorCode.UNAUTHORIZED, t.message ?: t.toString())
  }
}
