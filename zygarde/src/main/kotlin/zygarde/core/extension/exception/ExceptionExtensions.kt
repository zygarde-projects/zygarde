package zygarde.core.extension.exception

import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode

/**
 * @author leo
 */
private fun buildException(
  errorCode: ErrorCode,
  message: String?,
  args: Array<out Any?>
): BusinessException {
  if (message != null) {
    if (args.isNotEmpty()) {
      return BusinessException(errorCode, message, *args)
    }
    return BusinessException(errorCode, message)
  }
  return BusinessException(errorCode)
}

fun <T> T.errWhen(errorCode: ErrorCode, message: String? = null, vararg args: Any?, block: (t: T) -> Boolean): T {
  if (block.invoke(this)) {
    throw buildException(errorCode, message, args)
  }
  return this
}

fun <T> T?.errWhenNull(errorCode: ErrorCode, message: String? = null, vararg args: Any?): T {
  return this ?: throw buildException(errorCode, message, args)
}

fun <T, R> T.errWhenException(errorCode: ErrorCode, block: (t: T) -> R): R = try {
  block.invoke(this)
} catch (t: Throwable) {
  throw BusinessException(errorCode, t)
}

fun <R> nullWhenError(block: () -> R): R? = try {
  block.invoke()
} catch (t: Throwable) {
  null
}
