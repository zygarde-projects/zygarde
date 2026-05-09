package zygarde.lock.redis.impl

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript

class SimpleRedisLockTest {
  private val stringRedisTemplate = mockk<StringRedisTemplate>()
  private val lock = SimpleRedisLock({ "test-token" }, stringRedisTemplate)

  @Test
  fun `should acquire lock successfully`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } returns true

    val token = lock.acquire(listOf("key1"), "store", 10000L)

    token shouldNotBe null
    token shouldBe "test-token"
  }

  @Test
  fun `should return null when lock not available`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } returns false

    val token = lock.acquire(listOf("key1"), "store", 10000L)

    token shouldBe null
  }

  @Test
  fun `should release lock successfully`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } returns true

    val released = lock.release(listOf("key1"), "store", "test-token")

    released shouldBe true
  }

  @Test
  fun `should return false when release fails`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } returns false

    val released = lock.release(listOf("key1"), "store", "wrong-token")

    released shouldBe false
  }

  @Test
  fun `should refresh lock successfully`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } returns true

    val refreshed = lock.refresh(listOf("key1"), "store", "test-token", 10000L)

    refreshed shouldBe true
  }

  @Test
  fun `should return false when refresh fails`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } returns false

    val refreshed = lock.refresh(listOf("key1"), "store", "test-token", 10000L)

    refreshed shouldBe false
  }

  @Test
  fun `should return false and preserve interrupt when refresh is interrupted`() {
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } throws
      RedisSystemException("interrupted", InterruptedException())

    val refreshed = lock.refresh(listOf("key1"), "store", "test-token", 10000L)

    refreshed shouldBe false
    Thread.interrupted() shouldBe true
  }

  @Test
  fun `should rethrow non-interruption refresh exceptions`() {
    val exception = RedisSystemException("redis down", IllegalStateException("down"))
    every { stringRedisTemplate.execute(any<RedisScript<Boolean>>(), any<List<String>>(), *anyVararg()) } throws exception

    shouldThrow<RedisSystemException> {
      lock.refresh(listOf("key1"), "store", "test-token", 10000L)
    } shouldBe exception
  }
}
