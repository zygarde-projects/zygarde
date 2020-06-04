package zygarde.core.exception

enum class CommonErrorCode(
  override val code: String,
  override val message: String
) : ErrorCode {
  ERROR("999", "error")
}
