package zygarde.core.exception

enum class ApiErrorCode(
  override val code: String,
  override val message: String,
  override val httpStatus: Int,
) : HttpErrorCode {
  BAD_REQUEST("400", "Bad request", 400),
  UNAUTHORIZED("401", "Unauthorized", 401),
  NOT_FOUND("404", "Not found", 404),
  SERVER_ERROR("500", "Server error", 500)
}
