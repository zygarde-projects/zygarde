package zygarde.api.tracing

data class ApiTracingData(
  var apiId: String = "",
  val requestHeaders: String = "",
  val data: MutableMap<String, Any?> = mutableMapOf()
)
