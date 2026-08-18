package zygarde.api.exception

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.logging.LogLevel
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import jakarta.servlet.http.HttpServletRequest
import zygarde.api.tracing.ApiTracingContext
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException
import zygarde.core.exception.HttpErrorCode
import zygarde.core.extension.general.fallbackWhenNull
import zygarde.core.log.Loggable
import zygarde.json.toJsonString

/**
 * @author leo
 */
@ControllerAdvice
class ApiExceptionHandler : ApiExceptionResolver, Loggable {
  @Autowired
  private lateinit var messageSource: MessageSource

  @Autowired
  private lateinit var exceptionToBusinessExceptionMappers: List<ExceptionToBusinessExceptionMapper<*>>

  @Autowired
  private lateinit var apiErrorResponseFactory: ApiErrorResponseFactory

  @Value("\${zygarde.api.business-exception-log.level:INFO}")
  protected var businessExceptionLogLevel: LogLevel = LogLevel.INFO

  @Value("\${zygarde.api.business-exception-log.include-stack-trace:true}")
  protected var businessExceptionLogIncludeStackTrace: Boolean = true

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationError(e: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<Any> {
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
        "${objectError.objectName}.${objectError.field} ${objectError.defaultMessage}"
      }

    return ResponseEntity(
      apiErrorResponseFactory.create(
        errorCode = ApiErrorCode.BAD_REQUEST,
        messages = errorMessages,
        throwable = e,
        request = req
      ),
      HttpStatus.BAD_REQUEST
    )
  }

  @ExceptionHandler(BusinessException::class)
  fun handleBusinessException(e: BusinessException, req: HttpServletRequest): ResponseEntity<Any> {
    ApiTracingContext.getTracingData().exception = e
    logBusinessException(e)
    val code = e.code
    val res = apiErrorResponseFactory.create(
      errorCode = code,
      messages = listOfNotNull(e.message, e.cause?.message),
      throwable = e,
      request = req
    )
    return ResponseEntity(
      res,
      if (code is HttpErrorCode) HttpStatus.resolve(code.httpStatus) ?: HttpStatus.EXPECTATION_FAILED else HttpStatus.EXPECTATION_FAILED
    )
  }

  @ExceptionHandler(Throwable::class)
  override fun handleThrowable(t: Throwable, req: HttpServletRequest): ResponseEntity<Any> {
    ApiTracingContext.getTracingData().exception = t
    val cause = t.cause
    return if (cause != null) {
      handleThrowableInternal(cause, req) { handleThrowable(it, req) }
    } else {
      handleThrowableInternal(t, req) {
        logUnknownException(t, req)
        ResponseEntity(
          apiErrorResponseFactory.create(
            errorCode = ApiErrorCode.SERVER_ERROR,
            messages = listOf(t.message ?: t.javaClass.simpleName),
            throwable = t,
            request = req
          ),
          HttpStatus.INTERNAL_SERVER_ERROR
        )
      }
    }
  }

  protected fun handleThrowableInternal(
    t: Throwable,
    req: HttpServletRequest,
    onNoMatch: (t: Throwable) -> ResponseEntity<Any>
  ) = when (t) {
    is BusinessException -> handleBusinessException(t, req)
    else -> {
      val supported = exceptionToBusinessExceptionMappers.find { it.supported(t) }
      if (supported != null) {
        handleBusinessException(supported.handle(t), req)
      } else {
        onNoMatch(t)
      }
    }
  }

  protected open fun logUnknownException(t: Throwable, req: HttpServletRequest) {
    val tracingData = ApiTracingContext.getTracingData()
    val messages = listOfNotNull(
      "uri='${req.requestURI}",
      "requestHeaders=${tracingData.requestHeaders},",
      "exceptionMessage=${t.message}"
    )
    LOGGER.error(messages.joinToString(" ,"), t)
  }

  protected open fun logBusinessException(e: BusinessException) {
    val level = businessExceptionLogLevel
    if (level == LogLevel.OFF) return
    val message = businessExceptionLogMessage(e)
    val throwable: Throwable? = e.takeIf { businessExceptionLogIncludeStackTrace }
    when (level) {
      LogLevel.TRACE -> LOGGER.trace(message, throwable)
      LogLevel.DEBUG -> LOGGER.debug(message, throwable)
      LogLevel.INFO -> LOGGER.info(message, throwable)
      LogLevel.WARN -> LOGGER.warn(message, throwable)
      LogLevel.ERROR, LogLevel.FATAL -> LOGGER.error(message, throwable)
      LogLevel.OFF -> Unit
    }
  }

  protected open fun businessExceptionLogMessage(e: BusinessException): String {
    val tracingData = ApiTracingContext.getTracingData()
    return """${tracingData.apiId} ${e.code} ${e.message}
${tracingData.data.toJsonString()}
      """.trimMargin()
  }
}
