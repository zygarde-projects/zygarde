package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import zygarde.json.JacksonCommon
import zygarde.json.toJsonString
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * @author leo
 */
class JacksonCommonTest {
  @Test
  fun `should able to set objectMapper`() {
    val date = Date.from(LocalDateTime.of(2020, 1, 1, 1, 1).atZone(ZoneId.of("UTC")).toInstant())
    val source = mapOf("foo" to date)
    val json = source.toJsonString()
    json shouldBe """{"foo":"2020-01-01T01:01:00.000Z"}"""

    JacksonCommon.setObjectMapper(JsonMapper.builder().enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build())
    JacksonCommon.withObjectMapper { it.writeValueAsString(source) } shouldBe """{"foo":1577840460000}"""
  }
}
