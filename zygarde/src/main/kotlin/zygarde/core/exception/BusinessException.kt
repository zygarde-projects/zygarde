package zygarde.core.exception

import zygarde.extension.string.replaceByArgs

class BusinessException : RuntimeException {

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
}
