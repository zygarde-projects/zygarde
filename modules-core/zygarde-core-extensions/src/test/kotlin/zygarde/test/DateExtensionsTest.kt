package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.date.toDate
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class DateExtensionsTest {
  private val zoneId = ZoneId.of("UTC")
  private val timeZone = TimeZone.getTimeZone(zoneId)

  @Test
  fun `convert java8 LocalDate to java util date`() {
    val sdf = SimpleDateFormat("yyyy-MM-dd").apply { this.timeZone = this@DateExtensionsTest.timeZone }
    LocalDate.of(2000, 1, 1).toDate(zoneId) shouldBe sdf.parse("2000-01-01")
  }

  @Test
  fun `convert java8 LocalDateTime to java util date`() {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm").apply { this.timeZone = this@DateExtensionsTest.timeZone }
    LocalDateTime.of(2000, 1, 1, 12, 0).toDate(zoneId) shouldBe sdf.parse("2000-01-01 12:00")
  }
}
