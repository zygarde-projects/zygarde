package zygarde.api.exception.mapper

import com.fasterxml.jackson.module.kotlin.KotlinInvalidNullException
import org.springframework.stereotype.Component
import zygarde.api.exception.ExceptionToBusinessExceptionMapper
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException

@Component
class KotlinInvalidNullExceptionMapper : ExceptionToBusinessExceptionMapper<KotlinInvalidNullException>() {
  override fun supported(t: Throwable): Boolean = t is KotlinInvalidNullException

  override fun transform(t: KotlinInvalidNullException): BusinessException {
    return BusinessException(ApiErrorCode.BAD_REQUEST, "missing parameter '${t.kotlinPropertyName}'")
  }
}
