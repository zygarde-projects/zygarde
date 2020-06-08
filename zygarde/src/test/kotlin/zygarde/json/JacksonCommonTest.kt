import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import zygarde.json.JacksonCommon
import zygarde.json.toJsonString

/**
 * @author leo
 */
class JacksonCommonTest : StringSpec({
  "should able to set objectMapper" {
    val date = Date.from(LocalDateTime.of(2020, 1, 1, 1, 1).atZone(ZoneId.of("UTC")).toInstant())
    val source = mapOf("foo" to date)
    val json = source.toJsonString()
    json shouldBe """{"foo":"2020-01-01T01:01:00.000+0000"}"""

    JacksonCommon.setObjectMapper(jacksonObjectMapper().enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
    JacksonCommon.withObjectMapper { it.writeValueAsString(source) } shouldBe """{"foo":1577840460000}"""
  }
})
