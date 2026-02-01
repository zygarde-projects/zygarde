package zygarde.lock.retry

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import zygarde.lock.Interval
import zygarde.lock.Locked
import zygarde.lock.interval.IntervalConverter
import java.util.concurrent.TimeUnit

class DefaultRetryTemplateConverterTest {
  private val intervalConverter = mockk<IntervalConverter>()
  private val converter = DefaultRetryTemplateConverter(intervalConverter)

  @Test
  fun `should construct retry template with valid timeout and retry`() {
    val locked = Locked(
      timeout = Interval(value = "1000", unit = TimeUnit.MILLISECONDS),
      retry = Interval(value = "50", unit = TimeUnit.MILLISECONDS)
    )
    every { intervalConverter.toMillis(locked.timeout) } returns 1000L
    every { intervalConverter.toMillis(locked.retry) } returns 50L

    val result = converter.construct(locked)

    result shouldNotBe null
  }

  @Test
  fun `should return null when timeout is zero`() {
    val locked = Locked(
      timeout = Interval(value = "0", unit = TimeUnit.MILLISECONDS),
      retry = Interval(value = "50", unit = TimeUnit.MILLISECONDS)
    )
    every { intervalConverter.toMillis(locked.timeout) } returns 0L

    val result = converter.construct(locked)

    result shouldBe null
  }

  @Test
  fun `should return null when retry is zero`() {
    val locked = Locked(
      timeout = Interval(value = "1000", unit = TimeUnit.MILLISECONDS),
      retry = Interval(value = "0", unit = TimeUnit.MILLISECONDS)
    )
    every { intervalConverter.toMillis(locked.timeout) } returns 1000L
    every { intervalConverter.toMillis(locked.retry) } returns 0L

    val result = converter.construct(locked)

    result shouldBe null
  }

  @Test
  fun `should return null when timeout is negative`() {
    val locked = Locked(
      timeout = Interval(value = "-1", unit = TimeUnit.MILLISECONDS),
      retry = Interval(value = "50", unit = TimeUnit.MILLISECONDS)
    )
    every { intervalConverter.toMillis(locked.timeout) } returns -1L

    val result = converter.construct(locked)

    result shouldBe null
  }
}
