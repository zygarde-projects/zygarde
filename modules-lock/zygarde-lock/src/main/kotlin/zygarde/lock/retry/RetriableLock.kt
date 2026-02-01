package zygarde.lock.retry

import zygarde.lock.Lock
import zygarde.lock.exception.LockNotAvailableException
import org.springframework.retry.support.RetryTemplate

class RetriableLock(
  private val lock: Lock,
  private val retryTemplate: RetryTemplate
) : Lock {
  override fun acquire(keys: List<String>, storeId: String, expiration: Long): String? {
    return try {
      retryTemplate.execute<String, LockNotAvailableException> {
        val token = lock.acquire(keys, storeId, expiration)
        if (token.isNullOrEmpty()) {
          throw LockNotAvailableException("Lock not available for keys: $keys in store $storeId")
        }
        token
      }
    } catch (e: LockNotAvailableException) {
      null
    }
  }

  override fun release(keys: List<String>, storeId: String, token: String): Boolean {
    return lock.release(keys, storeId, token)
  }

  override fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean {
    return lock.refresh(keys, storeId, token, expiration)
  }
}
