package zygarde.ctx

object ApiVersionContext {
  private val apiVersionThreadLocal: ThreadLocal<Long> = ThreadLocal.withInitial { Long.MAX_VALUE }
  fun version(): Long = apiVersionThreadLocal.get()
  fun setVersion(apiVersion: Long) = apiVersionThreadLocal.set(apiVersion)
  fun clear() = apiVersionThreadLocal.remove()
}
