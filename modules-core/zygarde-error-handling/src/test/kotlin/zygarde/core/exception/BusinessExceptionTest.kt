package zygarde.core.exception

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BusinessExceptionTest {
  private enum class TestErrorCode(
    override val code: String,
    override val message: String,
  ) : ErrorCode {
    DOMAIN_ERROR("D001", "Domain error"),
  }

  @Test
  fun `should keep stack trace by default`() {
    // when
    val exception = BusinessException(TestErrorCode.DOMAIN_ERROR)

    // then
    exception.stackTrace.isNotEmpty() shouldBe true
    exception.message shouldBe "Domain error"
  }

  @Test
  fun `noStackTrace should create exception without stack trace and format message args`() {
    // when
    val exception = BusinessException.noStackTrace(TestErrorCode.DOMAIN_ERROR, "item {} not found", 42)

    // then
    exception.stackTrace.isEmpty() shouldBe true
    exception.code shouldBe TestErrorCode.DOMAIN_ERROR
    exception.message shouldBe "item 42 not found"
    exception.cause shouldBe null
  }

  @Test
  fun `noStackTrace should keep cause and fall back to error code message`() {
    // given
    val cause = IllegalStateException("root cause")

    // when
    val exception = BusinessException.noStackTrace(TestErrorCode.DOMAIN_ERROR, cause)

    // then
    exception.stackTrace.isEmpty() shouldBe true
    exception.message shouldBe "Domain error"
    exception.cause shouldBe cause
  }

  @Test
  fun `should allow subclassing`() {
    // given
    class MyBusinessException : BusinessException(TestErrorCode.DOMAIN_ERROR, null, null, false)

    // when
    val exception = MyBusinessException()

    // then
    exception.code shouldBe TestErrorCode.DOMAIN_ERROR
    exception.stackTrace.isEmpty() shouldBe true
  }
}
