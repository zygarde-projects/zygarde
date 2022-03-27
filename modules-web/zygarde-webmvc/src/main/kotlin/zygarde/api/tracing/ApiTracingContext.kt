package zygarde.api.tracing

import zygarde.json.toJsonString
import javax.servlet.http.HttpServletRequest

object ApiTracingContext {
  private val tracingDataThreadLocal: ThreadLocal<ApiTracingData> = ThreadLocal.withInitial { ApiTracingData() }

  fun getTracingData() = tracingDataThreadLocal.get()

  fun putData(data: Map<String, Any?>) {
    tracingDataThreadLocal.get().data.putAll(data)
  }

  fun putData(k: String, v: Any?) {
    tracingDataThreadLocal.get().data[k] = v
  }

  fun setApiId(apiId: String) {
    tracingDataThreadLocal.get().apiId = apiId
  }

  fun trace(req: HttpServletRequest, block: () -> Unit) {
    try {
      tracingDataThreadLocal.set(ApiTracingData(requestHeaders = req.getHeadersAsString()))
      block.invoke()
    } finally {
      tracingDataThreadLocal.remove()
    }
  }

  private fun HttpServletRequest.getHeadersAsString(): String {
    return headerNames.toList().associateWith { getHeaders(it).toList() }.toJsonString()
  }
}
