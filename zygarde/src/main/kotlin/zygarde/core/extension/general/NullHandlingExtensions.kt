package zygarde.core.extension.general

fun <T> T?.fallbackWhenNull(fallback: T): T {
  return this.fallbackWhenNull { fallback }
}

fun <T> T?.fallbackWhenNull(fallbackBuilder: () -> T): T {
  return this ?: fallbackBuilder.invoke()
}
