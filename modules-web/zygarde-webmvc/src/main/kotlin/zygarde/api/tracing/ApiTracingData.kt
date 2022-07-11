package zygarde.api.tracing

data class ApiTracingData(
  var apiId: String = "",
  val requestHeaders: String = "",
  var exception: Throwable? = null,
  val data: MutableMap<String, Any?> = mutableMapOf()
)
