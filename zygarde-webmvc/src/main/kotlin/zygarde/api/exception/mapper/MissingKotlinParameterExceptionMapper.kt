package zygarde.api.exception.mapper

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.springframework.stereotype.Component
import zygarde.api.exception.ExceptionToBusinessExceptionMapper
import zygarde.api.ApiErrorCode
import zygarde.core.exception.BusinessException

@Component
class MissingKotlinParameterExceptionMapper : ExceptionToBusinessExceptionMapper<MissingKotlinParameterException>() {
  override fun supported(t: Throwable): Boolean = t is MissingKotlinParameterException

  override fun transform(t: MissingKotlinParameterException): BusinessException {
    return BusinessException(ApiErrorCode.BAD_REQUEST, "missing parameter '${t.parameter.name}'")
  }
}
