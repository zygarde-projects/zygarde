package zygarde.lock.redis.impl

import zygarde.lock.AbstractSimpleLock
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

class SimpleRedisLock(
  tokenSupplier: () -> String,
  private val stringRedisTemplate: StringRedisTemplate
) : AbstractSimpleLock(tokenSupplier) {
  private val log = LoggerFactory.getLogger(SimpleRedisLock::class.java)

  private val lockScript = DefaultRedisScript(LOCK_SCRIPT, Boolean::class.javaObjectType)
  private val lockReleaseScript = DefaultRedisScript(LOCK_RELEASE_SCRIPT, Boolean::class.javaObjectType)
  private val lockRefreshScript = DefaultRedisScript(LOCK_REFRESH_SCRIPT, Boolean::class.javaObjectType)

  override fun acquire(key: String, storeId: String, token: String, expiration: Long): String? {
    val singletonKeyList = listOf("$storeId:$key")
    val locked = stringRedisTemplate.execute(lockScript, singletonKeyList, token, expiration.toString()) ?: false
    log.debug("Tried to acquire lock for key {} with token {} in store {}. Locked: {}", key, token, storeId, locked)
    return if (locked) token else null
  }

  override fun release(key: String, storeId: String, token: String): Boolean {
    val singletonKeyList = listOf("$storeId:$key")
    val released = stringRedisTemplate.execute(lockReleaseScript, singletonKeyList, token) ?: false
    if (released) {
      log.debug("Release script deleted the record for key {} with token {} in store {}", key, token, storeId)
    } else {
      log.error("Release script failed for key {} with token {} in store {}", key, token, storeId)
    }
    return released
  }

  override fun refresh(key: String, storeId: String, token: String, expiration: Long): Boolean {
    val singletonKeyList = listOf("$storeId:$key")
    var refreshed = false
    try {
      refreshed = stringRedisTemplate.execute(lockRefreshScript, singletonKeyList, token, expiration.toString()) ?: false
      if (refreshed) {
        log.debug("Refresh script updated the expiration for key {} with token {} in store {} to {}", key, token, storeId, expiration)
      } else {
        log.debug(
          "Refresh script failed to update expiration for key {} with token {} in store {} with expiration: {}",
          key,
          token,
          storeId,
          expiration
        )
      }
    } catch (e: RedisSystemException) {
      if (e.cause is InterruptedException || e.cause?.javaClass?.name == "io.lettuce.core.RedisCommandInterruptedException") {
        log.debug(
          "Refresh script thread interrupted to update expiration for key {} with token {} in store {} with expiration: {}",
          key,
          token,
          storeId,
          expiration
        )
        Thread.currentThread().interrupt()
      } else {
        throw e
      }
    }
    return refreshed
  }

  companion object {
    private const val LOCK_SCRIPT =
      "return redis.call('SET', KEYS[1], ARGV[1], 'PX', tonumber(ARGV[2]), 'NX') and true or false"

    private const val LOCK_RELEASE_SCRIPT =
      "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
        "    return redis.call('DEL', KEYS[1]) == 1\n" +
        "end\n" +
        "return false"

    private const val LOCK_REFRESH_SCRIPT =
      "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
        "    redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[2]))\n" +
        "    return true\n" +
        "end\n" +
        "return false"
  }
}
