package zygarde.api.exception

import zygarde.core.exception.BusinessException

abstract class ExceptionToBusinessExceptionMapper<T : Throwable> {

  @Suppress("UNCHECKED_CAST")
  fun handle(t: Throwable): BusinessException = transform(t as T)

  abstract fun supported(t: Throwable): Boolean

  protected abstract fun transform(t: T): BusinessException
}
