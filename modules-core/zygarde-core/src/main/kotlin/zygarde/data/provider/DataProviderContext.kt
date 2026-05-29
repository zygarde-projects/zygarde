package zygarde.data.provider

class DataProviderContext(
  private val values: Map<String, Any?> = emptyMap(),
) {
  operator fun get(name: String): Any? = values[name]

  companion object {
    val EMPTY = DataProviderContext()
  }
}
