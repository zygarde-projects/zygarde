package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.utils.DateUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateUtilsTest {
  @Test
  fun overlap() {
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    DateUtils.overlap(
      LocalDateTime.parse("2022-01-01 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-02 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-02 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-03 00:00:00", dateFormat),
    ) shouldBe true

    DateUtils.overlap(
      LocalDateTime.parse("2022-01-01 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-05 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-02 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-03 00:00:00", dateFormat),
    ) shouldBe true

    DateUtils.overlap(
      LocalDateTime.parse("2022-01-02 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-03 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-01 00:00:00", dateFormat),
      LocalDateTime.parse("2022-01-05 00:00:00", dateFormat),
    ) shouldBe true
  }
}
