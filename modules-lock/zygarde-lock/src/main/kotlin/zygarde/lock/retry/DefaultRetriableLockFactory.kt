package zygarde.lock.retry

import zygarde.lock.Lock
import zygarde.lock.Locked

class DefaultRetriableLockFactory(
  private val retryTemplateConverter: RetryTemplateConverter
) : RetriableLockFactory {
  override fun generate(lock: Lock, locked: Locked): Lock {
    val retryTemplate = retryTemplateConverter.construct(locked) ?: return lock
    return RetriableLock(lock, retryTemplate)
  }
}
