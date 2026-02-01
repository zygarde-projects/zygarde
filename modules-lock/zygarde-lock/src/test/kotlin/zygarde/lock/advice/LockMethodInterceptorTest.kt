package zygarde.lock.advice

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import zygarde.lock.Lock
import zygarde.lock.Locked
import zygarde.lock.exception.DistributedLockException

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = [LockMethodInterceptorTest.TestApp::class]
)
class LockMethodInterceptorTest {
  @Autowired
  lateinit var lockedService: LockedService

  @Autowired
  lateinit var testLock: Lock

  @BeforeEach
  fun setup() {
    clearMocks(testLock)
  }

  @Test
  fun `should acquire and release lock around method execution`() {
    every { testLock.acquire(any(), any(), any()) } returns "test-token"
    every { testLock.release(any(), any(), any()) } returns true

    val result = lockedService.doWork("hello")

    result shouldBe "hello"
    verify(exactly = 1) { testLock.acquire(any(), eq("distributed_lock"), any()) }
    verify(exactly = 1) { testLock.release(any(), eq("distributed_lock"), eq("test-token")) }
  }

  @Test
  fun `should throw when lock cannot be acquired`() {
    every { testLock.acquire(any(), any(), any()) } returns null

    assertThrows<DistributedLockException> {
      lockedService.doWork("hello")
    }
  }

  @Test
  fun `should return null when throwing is false and lock fails`() {
    every { testLock.acquire(any(), any(), any()) } returns null

    val result = lockedService.doWorkNonThrowing("hello")

    result shouldBe null
  }

  @Test
  fun `should throw when throwing is false but return type is primitive`() {
    every { testLock.acquire(any(), any(), any()) } returns null

    assertThrows<DistributedLockException> {
      lockedService.doWorkNonThrowingPrimitive()
    }
  }

  open class LockedService {
    @Locked(expression = "'test-key'")
    open fun doWork(input: String): String = input

    @Locked(expression = "'test-key'", throwing = false)
    open fun doWorkNonThrowing(input: String): String? = input

    @Locked(expression = "'test-key'", throwing = false)
    open fun doWorkNonThrowingPrimitive(): Int = 42
  }

  @SpringBootApplication
  open class TestApp {
    @Bean
    open fun testLock(): Lock = mockk(relaxed = true)

    @Bean
    open fun lockTypeResolver(testLock: Lock): LockTypeResolver = LockTypeResolver { testLock }

    @Bean
    open fun lockedService(): LockedService = LockedService()
  }
}
