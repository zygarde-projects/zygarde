package zygarde.core.exception

interface HttpErrorCode : ErrorCode {
  val httpStatus: Int
}
