package zygarde.api.exception

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import zygarde.api.ApiErrorCode
import zygarde.core.exception.BusinessException
import zygarde.core.extension.general.fallbackWhenNull
import zygarde.core.log.Loggable
import zygarde.data.api.ApiErrorResponse

/**
 * @author leo
 */
@ControllerAdvice
class ApiExceptionHandler : Loggable {

  @Autowired
  private lateinit var messageSource: MessageSource
  @Autowired
  private lateinit var exceptionToBusinessExceptionMappers: List<ExceptionToBusinessExceptionMapper<*>>

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
    val locale = LocaleContextHolder.getLocale()
    val bindingResult = e.bindingResult
    val errorMessages = bindingResult.fieldErrors
      .map { objectError ->
        objectError
          .codes
          .fallbackWhenNull(emptyArray())
          .forEach {
            try {
              return@map messageSource.getMessage(it, objectError.arguments, locale)
            } catch (ex: NoSuchMessageException) {
              LOGGER.debug(ex.message)
            }
          }
        "${objectError.field} ${objectError.defaultMessage}"
      }

    return ResponseEntity(
      ApiErrorResponse(
        errorCode = ApiErrorCode.BAD_REQUEST,
        messages = errorMessages
      ),
      HttpStatus.BAD_REQUEST
    )
  }

  @ExceptionHandler(BusinessException::class)
  fun handleBusinessException(e: BusinessException): ResponseEntity<ApiErrorResponse> {
    LOGGER.error(e.message, e)
    val code = e.code
    val res = ApiErrorResponse(
      errorCode = code,
      messages = listOfNotNull(e.message, e.cause?.message)
    )
    return ResponseEntity(res, if (code is ApiErrorCode) code.httpStatus else HttpStatus.EXPECTATION_FAILED)
  }

  @ExceptionHandler(Throwable::class)
  fun handleThrowable(t: Throwable): ResponseEntity<ApiErrorResponse> {
    val cause = t.cause
    return if (cause != null) {
      handleThrowableInternal(cause, this::handleThrowable)
    } else {
      handleThrowableInternal(t) {
        LOGGER.error(t.message, t)
        ResponseEntity(
          ApiErrorResponse(
            errorCode = ApiErrorCode.SERVER_ERROR,
            messages = listOf(t.message ?: t.javaClass.simpleName)
          ),
          HttpStatus.INTERNAL_SERVER_ERROR
        )
      }
    }
  }

  protected fun handleThrowableInternal(
    t: Throwable,
    onNoMatch: (t: Throwable) -> ResponseEntity<ApiErrorResponse>
  ) = when (t) {
    is BusinessException -> handleBusinessException(t)
    else -> {
      val supported = exceptionToBusinessExceptionMappers.find { it.supported(t) }
      if (supported != null) {
        handleBusinessException(supported.handle(t))
      } else {
        onNoMatch(t)
      }
    }
  }
}
