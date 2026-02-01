package zygarde.lock.interval

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import zygarde.lock.Interval

class BeanFactoryAwareIntervalConverterTest {
  private val beanFactory = mockk<ConfigurableBeanFactory>()
  private val converter = BeanFactoryAwareIntervalConverter(beanFactory)

  @Test
  fun `should convert milliseconds interval`() {
    val interval = Interval(value = "500", unit = TimeUnit.MILLISECONDS)
    every { beanFactory.resolveEmbeddedValue("500") } returns "500"

    val result = converter.toMillis(interval)

    result shouldBe 500L
  }

  @Test
  fun `should convert seconds interval to milliseconds`() {
    val interval = Interval(value = "10", unit = TimeUnit.SECONDS)
    every { beanFactory.resolveEmbeddedValue("10") } returns "10"

    val result = converter.toMillis(interval)

    result shouldBe 10000L
  }

  @Test
  fun `should resolve property placeholder`() {
    val interval = Interval(value = "\${lock.timeout}", unit = TimeUnit.MILLISECONDS)
    every { beanFactory.resolveEmbeddedValue("\${lock.timeout}") } returns "200"

    val result = converter.toMillis(interval)

    result shouldBe 200L
  }

  @Test
  fun `should throw on blank resolved value`() {
    val interval = Interval(value = "\${lock.timeout}", unit = TimeUnit.MILLISECONDS)
    every { beanFactory.resolveEmbeddedValue("\${lock.timeout}") } returns ""

    assertThrows<IllegalArgumentException> {
      converter.toMillis(interval)
    }
  }

  @Test
  fun `should throw on non-numeric resolved value`() {
    val interval = Interval(value = "abc", unit = TimeUnit.MILLISECONDS)
    every { beanFactory.resolveEmbeddedValue("abc") } returns "abc"

    assertThrows<IllegalArgumentException> {
      converter.toMillis(interval)
    }
  }
}
