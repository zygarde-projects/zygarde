package zygarde.core.di

inline fun <reified T : Any> bean(): T {
  return DiServiceContext.bean()
}

inline fun <reified T : Any> autowired(): Lazy<T> = lazy { bean<T>() }
