package zygarde.test

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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
    json shouldBe """{"foo":"2020-01-01T01:01:00.000+00:00"}"""

    JacksonCommon.setObjectMapper(jacksonObjectMapper().enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
    JacksonCommon.withObjectMapper { it.writeValueAsString(source) } shouldBe """{"foo":1577840460000}"""
  }
}
