package zygarde.codegen

enum class RequestBodyContentType(
  val value: String?
) {
  DEFAULT(null),
  JSON_MERGE_PATCH("application/merge-patch+json"),
}
