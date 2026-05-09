package zygarde.core.extension.date

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DateExtensionsTest {
  @Test
  fun `toDate should convert local date and local date time with provided zone`() {
    val zone = ZoneId.of("UTC")

    LocalDate.of(2026, 5, 9).toDate(zone).toInstant().toString() shouldBe "2026-05-09T00:00:00Z"
    LocalDateTime.of(2026, 5, 9, 12, 34, 56).toDate(zone).toInstant().toString() shouldBe "2026-05-09T12:34:56Z"
  }

  @Test
  fun `LocalDate isBetween should respect inclusive and exclusive boundaries`() {
    val from = LocalDate.of(2026, 5, 1)
    val middle = LocalDate.of(2026, 5, 9)
    val to = LocalDate.of(2026, 5, 31)

    middle.isBetween(from, to) shouldBe true
    from.isBetween(from, to) shouldBe true
    to.isBetween(from, to) shouldBe true
    from.isBetween(from, to, includingFrom = false) shouldBe false
    to.isBetween(from, to, includingTo = false) shouldBe false
    LocalDate.of(2026, 4, 30).isBetween(from, to) shouldBe false
  }

  @Test
  fun `LocalDateTime isBetween should respect inclusive and exclusive boundaries`() {
    val from = LocalDateTime.of(2026, 5, 1, 0, 0)
    val middle = LocalDateTime.of(2026, 5, 9, 12, 0)
    val to = LocalDateTime.of(2026, 5, 31, 23, 59)

    middle.isBetween(from, to) shouldBe true
    from.isBetween(from, to) shouldBe true
    to.isBetween(from, to) shouldBe true
    from.isBetween(from, to, includingFrom = false) shouldBe false
    to.isBetween(from, to, includingTo = false) shouldBe false
    LocalDateTime.of(2026, 6, 1, 0, 0).isBetween(from, to) shouldBe false
  }
}
