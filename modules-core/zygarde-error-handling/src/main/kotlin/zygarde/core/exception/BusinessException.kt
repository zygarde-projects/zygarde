package zygarde.core.exception

import zygarde.core.extension.string.replaceByArgs

open class BusinessException : RuntimeException {
  val code: ErrorCode

  constructor(code: ErrorCode) : super(code.message) {
    this.code = code
  }

  constructor(code: ErrorCode, cause: Throwable) : super(code.message, cause) {
    this.code = code
  }

  constructor(code: ErrorCode, message: String, vararg args: Any?) : super(message.replaceByArgs(*args)) {
    this.code = code
  }

  /**
   * Full-control constructor for subclasses: pass [writableStackTrace] = false to create an
   * exception without capturing a stack trace (cheap to construct, nothing to print in logs).
   */
  protected constructor(
    code: ErrorCode,
    message: String?,
    cause: Throwable?,
    writableStackTrace: Boolean,
    vararg args: Any?,
  ) : super((message ?: code.message).replaceByArgs(*args), cause, true, writableStackTrace) {
    this.code = code
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun noStackTrace(code: ErrorCode, cause: Throwable? = null): BusinessException =
      BusinessException(code = code, message = null, cause = cause, writableStackTrace = false)

    @JvmStatic
    fun noStackTrace(code: ErrorCode, message: String, vararg args: Any?): BusinessException =
      BusinessException(code = code, message = message, cause = null, writableStackTrace = false, args = args)
  }
}
