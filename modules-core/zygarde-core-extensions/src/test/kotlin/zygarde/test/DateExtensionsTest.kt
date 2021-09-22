package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.date.toDate
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime

class DateExtensionsTest {
  @Test
  fun `convert java8 LocalDate to java util date`() {
    LocalDate.of(2000, 1, 1).toDate() shouldBe SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01")
  }

  @Test
  fun `convert java8 LocalDateTime to java util date`() {
    LocalDateTime.of(2000, 1, 1, 12, 0).toDate() shouldBe SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2000-01-01 12:00")
  }
}
