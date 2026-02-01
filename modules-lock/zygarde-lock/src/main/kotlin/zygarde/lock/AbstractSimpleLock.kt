package zygarde.lock

import org.springframework.util.Assert

abstract class AbstractSimpleLock(
  private val tokenSupplier: () -> String
) : Lock {
  override fun acquire(keys: List<String>, storeId: String, expiration: Long): String? {
    Assert.isTrue(keys.size == 1, "Cannot acquire lock for multiple keys with this lock")
    val token = tokenSupplier()
    check(token.isNotEmpty()) { "Cannot lock with empty token" }
    return acquire(keys[0], storeId, token, expiration)
  }

  override fun release(keys: List<String>, storeId: String, token: String): Boolean {
    Assert.isTrue(keys.size == 1, "Cannot release lock for multiple keys with this lock")
    return release(keys[0], storeId, token)
  }

  override fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean {
    Assert.isTrue(keys.size == 1, "Cannot refresh lock for multiple keys with this lock")
    return refresh(keys[0], storeId, token, expiration)
  }

  protected abstract fun acquire(key: String, storeId: String, token: String, expiration: Long): String?

  protected abstract fun release(key: String, storeId: String, token: String): Boolean

  protected abstract fun refresh(key: String, storeId: String, token: String, expiration: Long): Boolean
}
