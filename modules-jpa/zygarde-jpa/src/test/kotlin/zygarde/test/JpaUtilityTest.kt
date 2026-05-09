package zygarde.test

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import zygarde.data.api.toPageDto
import zygarde.jpa.converter.LocalDateConverter
import zygarde.jpa.converter.LocalDateTimeConverter
import zygarde.jpa.converter.StringListToJsonStringConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JpaUtilityTest {
  private class TestLocalDateConverter : LocalDateConverter(DateTimeFormatter.ISO_LOCAL_DATE)

  private class TestLocalDateTimeConverter : LocalDateTimeConverter(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  @Test
  fun `toPageDto should convert Spring page metadata and items`() {
    val page = PageImpl(listOf(1, 2, 3), PageRequest.of(1, 3), 10)

    val dto = page.toPageDto { "item-$it" }

    dto.atPage shouldBe 2
    dto.totalPages shouldBe 4
    dto.totalCount shouldBe 10
    dto.items shouldContainExactly listOf("item-1", "item-2", "item-3")
  }

  @Test
  fun `JSON list converter should handle null empty and populated values`() {
    val converter = StringListToJsonStringConverter()

    converter.convertToDatabaseColumn(null) shouldBe "[]"
    converter.convertToDatabaseColumn(listOf("a", "b")) shouldBe """["a","b"]"""
    converter.convertToEntityAttribute(null) shouldContainExactly emptyList()
    converter.convertToEntityAttribute("""["a","b"]""") shouldContainExactly listOf("a", "b")
  }

  @Test
  fun `temporal converters should format parse and ignore empty database values`() {
    val dateConverter = TestLocalDateConverter()
    val dateTimeConverter = TestLocalDateTimeConverter()
    val date = LocalDate.of(2026, 5, 9)
    val dateTime = LocalDateTime.of(2026, 5, 9, 3, 30, 0)

    dateConverter.convertToDatabaseColumn(date) shouldBe "2026-05-09"
    dateConverter.convertToDatabaseColumn(null) shouldBe null
    dateConverter.convertToEntityAttribute("2026-05-09") shouldBe date
    dateConverter.convertToEntityAttribute("") shouldBe null
    dateConverter.convertToEntityAttribute(null) shouldBe null
    dateTimeConverter.convertToDatabaseColumn(dateTime) shouldBe "2026-05-09T03:30:00"
    dateTimeConverter.convertToDatabaseColumn(null) shouldBe null
    dateTimeConverter.convertToEntityAttribute("2026-05-09T03:30:00") shouldBe dateTime
    dateTimeConverter.convertToEntityAttribute("") shouldBe null
    dateTimeConverter.convertToEntityAttribute(null) shouldBe null
  }
}
