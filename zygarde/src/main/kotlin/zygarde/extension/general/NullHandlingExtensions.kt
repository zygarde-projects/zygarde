package zygarde.extension.general

fun <T> T?.fallbackWhenNull(fallback: T): T {
  return fallbackWhenNull { fallback }
}
fun <T> T?.fallbackWhenNull(fallbackBuilder: () -> T): T {
  return this ?: fallbackBuilder.invoke()
}
