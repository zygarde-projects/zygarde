package zygarde.lock.retry

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import zygarde.lock.Lock
import zygarde.lock.exception.LockNotAvailableException

class RetriableLockTest {
  private val innerLock = mockk<Lock>()

  @Test
  fun `should acquire lock on first attempt`() {
    val retryTemplate = RetryTemplate().apply {
      setRetryPolicy(SimpleRetryPolicy(3, mapOf(LockNotAvailableException::class.java to true)))
      setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = 10 })
    }
    val retriableLock = RetriableLock(innerLock, retryTemplate)

    every { innerLock.acquire(listOf("key1"), "store", 1000L) } returns "token123"

    val token = retriableLock.acquire(listOf("key1"), "store", 1000L)

    token shouldBe "token123"
    verify(exactly = 1) { innerLock.acquire(listOf("key1"), "store", 1000L) }
  }

  @Test
  fun `should retry and acquire lock on second attempt`() {
    val retryTemplate = RetryTemplate().apply {
      setRetryPolicy(SimpleRetryPolicy(3, mapOf(LockNotAvailableException::class.java to true)))
      setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = 10 })
    }
    val retriableLock = RetriableLock(innerLock, retryTemplate)

    every { innerLock.acquire(listOf("key1"), "store", 1000L) } returnsMany listOf(null, "token456")

    val token = retriableLock.acquire(listOf("key1"), "store", 1000L)

    token shouldBe "token456"
    verify(exactly = 2) { innerLock.acquire(listOf("key1"), "store", 1000L) }
  }

  @Test
  fun `should return null when retries exhausted`() {
    val retryTemplate = RetryTemplate().apply {
      setRetryPolicy(SimpleRetryPolicy(2, mapOf(LockNotAvailableException::class.java to true)))
      setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = 10 })
    }
    val retriableLock = RetriableLock(innerLock, retryTemplate)

    every { innerLock.acquire(listOf("key1"), "store", 1000L) } returns null

    val token = retriableLock.acquire(listOf("key1"), "store", 1000L)

    token shouldBe null
  }

  @Test
  fun `should delegate release to inner lock`() {
    val retryTemplate = RetryTemplate()
    val retriableLock = RetriableLock(innerLock, retryTemplate)

    every { innerLock.release(listOf("key1"), "store", "token") } returns true

    val result = retriableLock.release(listOf("key1"), "store", "token")

    result shouldBe true
    verify(exactly = 1) { innerLock.release(listOf("key1"), "store", "token") }
  }

  @Test
  fun `should delegate refresh to inner lock`() {
    val retryTemplate = RetryTemplate()
    val retriableLock = RetriableLock(innerLock, retryTemplate)

    every { innerLock.refresh(listOf("key1"), "store", "token", 5000L) } returns true

    val result = retriableLock.refresh(listOf("key1"), "store", "token", 5000L)

    result shouldBe true
    verify(exactly = 1) { innerLock.refresh(listOf("key1"), "store", "token", 5000L) }
  }
}
