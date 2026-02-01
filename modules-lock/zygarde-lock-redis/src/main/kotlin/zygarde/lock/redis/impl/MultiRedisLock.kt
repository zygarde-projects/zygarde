package zygarde.lock.redis.impl

import zygarde.lock.Lock
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

/**
 * Redis-based distributed lock supporting multiple keys with atomic Lua scripts.
 *
 * Note: The Lua scripts use `unpack(KEYS)` which is subject to Lua's stack size limit
 * (typically ~7000 elements). Do not use this with extremely large key lists.
 * Also, all keys must reside on the same Redis node (or cluster hash slot) for
 * the Lua scripts to execute atomically.
 */
class MultiRedisLock(
  private val stringRedisTemplate: StringRedisTemplate,
  private val tokenSupplier: () -> String = { UUID.randomUUID().toString() }
) : Lock {
  private val log = LoggerFactory.getLogger(MultiRedisLock::class.java)

  private val lockScript = DefaultRedisScript(LOCK_SCRIPT, Boolean::class.javaObjectType)
  private val lockReleaseScript = DefaultRedisScript(LOCK_RELEASE_SCRIPT, Boolean::class.javaObjectType)
  private val lockRefreshScript = DefaultRedisScript(LOCK_REFRESH_SCRIPT, Boolean::class.javaObjectType)

  override fun acquire(keys: List<String>, storeId: String, expiration: Long): String? {
    val keysWithStoreIdPrefix = keys.map { key -> "$storeId:$key" }
    val token = tokenSupplier()
    check(token.isNotEmpty()) { "Cannot lock with empty token" }
    val locked = stringRedisTemplate.execute(lockScript, keysWithStoreIdPrefix, token, expiration.toString()) ?: false
    log.debug("Tried to acquire lock for keys {} in store {} with token {}. Locked: {}", keys, storeId, token, locked)
    return if (locked) token else null
  }

  override fun release(keys: List<String>, storeId: String, token: String): Boolean {
    val keysWithStoreIdPrefix = keys.map { key -> "$storeId:$key" }
    val released = stringRedisTemplate.execute(lockReleaseScript, keysWithStoreIdPrefix, token) ?: false
    if (released) {
      log.debug("Release script deleted the record for keys {} with token {} in store {}", keys, token, storeId)
    } else {
      log.error("Release script failed for keys {} with token {} in store {}", keys, token, storeId)
    }
    return released
  }

  override fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean {
    val keysWithStoreIdPrefix = keys.map { key -> "$storeId:$key" }
    var refreshed = false
    try {
      refreshed = stringRedisTemplate.execute(lockRefreshScript, keysWithStoreIdPrefix, token, expiration.toString()) ?: false
      if (refreshed) {
        log.debug("Refresh script refreshed the expiration for keys {} with token {} in store {}", keys, token, storeId)
      } else {
        log.debug("Refresh script failed to update expiration for keys {} with token {} in store {}", keys, token, storeId)
      }
    } catch (e: RedisSystemException) {
      if (e.cause is InterruptedException || e.cause?.javaClass?.name == "io.lettuce.core.RedisCommandInterruptedException") {
        log.debug(
          "Refresh script thread interrupted to update expiration for keys {} with token {} in store {} with expiration: {}",
          keys,
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
      "local expiration = tonumber(ARGV[2])\n" +
        "local locked = {}\n" +
        "for _, key in ipairs(KEYS) do\n" +
        "    if redis.call('SET', key, ARGV[1], 'PX', expiration, 'NX') then\n" +
        "        locked[#locked + 1] = key\n" +
        "    else\n" +
        "        for _, locked_key in ipairs(locked) do\n" +
        "            redis.call('DEL', locked_key)\n" +
        "        end\n" +
        "        return false\n" +
        "    end\n" +
        "end\n" +
        "return true\n"

    private const val LOCK_RELEASE_SCRIPT =
      "for _, key in pairs(KEYS) do\n" +
        "    if redis.call('GET', key) ~= ARGV[1] then\n" +
        "        return false\n" +
        "    end\n" +
        "end\n" +
        "redis.call('DEL', unpack(KEYS))\n" +
        "return true\n"

    private const val LOCK_REFRESH_SCRIPT =
      "for _, key in pairs(KEYS) do\n" +
        "    local value = redis.call('GET', key)\n" +
        "    if (value == nil or value ~= ARGV[1]) then\n" +
        "        return false\n" +
        "    end\n" +
        "end\n" +
        "for _, key in pairs(KEYS) do\n" +
        "    redis.call('PEXPIRE', key, ARGV[2])\n" +
        "end\n" +
        "return true"
  }
}
