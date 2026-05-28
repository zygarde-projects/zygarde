package zygarde.sql.api.exception

import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessApiUsageException
import zygarde.api.exception.ExceptionToBusinessExceptionMapper
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException

class SqlDataAccessExceptionMapper : ExceptionToBusinessExceptionMapper<Throwable>() {
  override fun supported(t: Throwable): Boolean {
    return t is DataAccessException || t.isSqlInputException()
  }

  override fun transform(t: Throwable): BusinessException {
    return when {
      t is DataIntegrityViolationException -> BusinessException(ApiErrorCode.CONFLICT, t.message ?: t.toString())
      t is InvalidDataAccessApiUsageException -> BusinessException(ApiErrorCode.BAD_REQUEST, t.message ?: t.toString())
      t.isSqlInputException() -> BusinessException(ApiErrorCode.BAD_REQUEST, t.message ?: t.toString())
      else -> BusinessException(ApiErrorCode.SERVER_ERROR, t.message ?: t.toString())
    }
  }

  private fun Throwable.isSqlInputException(): Boolean {
    return this is IllegalArgumentException && message?.let {
      it.startsWith("Missing SQL parameter '") ||
        it.startsWith("SQL parameter '") ||
        it.startsWith("SQL column '") ||
        it.startsWith("Unsupported SQL value conversion") ||
        it.startsWith("Cannot convert ")
    } == true
  }
}
