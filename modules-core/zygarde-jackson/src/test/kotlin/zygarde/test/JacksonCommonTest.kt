package zygarde.test

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import zygarde.json.JacksonCommon
import zygarde.json.toJsonString
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * @author leo
 */
class JacksonCommonTest {
  data class IdsReq(val ids: List<Int>)

  data class NameReq(val name: String, val age: Int = 18)

  @Test
  fun `should able to set objectMapper`() {
    val date = Date.from(LocalDateTime.of(2020, 1, 1, 1, 1).atZone(ZoneId.of("UTC")).toInstant())
    val source = mapOf("foo" to date)
    val json = source.toJsonString()
    json shouldBe """{"foo":"2020-01-01T01:01:00.000Z"}"""

    JacksonCommon.setObjectMapper(JsonMapper.builder().enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build())
    JacksonCommon.withObjectMapper { it.writeValueAsString(source) } shouldBe """{"foo":1577840460000}"""
  }

  @Test
  fun `should keep caller configured KotlinModule`() {
    JacksonCommon.setObjectMapper(
      JsonMapper.builder()
        .addModule(kotlinModule { disable(KotlinFeature.StrictNullChecks) })
        .build(),
    )

    val req = JacksonCommon.objectMapper().readValue<IdsReq>("""{"ids":[1,null]}""")
    req.ids shouldContainExactly listOf(1, null)
  }

  @Test
  fun `should add KotlinModule when mapper has none`() {
    JacksonCommon.setObjectMapper(JsonMapper.builder().build())

    JacksonCommon.objectMapper().registeredModules().count { it is KotlinModule } shouldBe 1
    JacksonCommon.objectMapper().readValue<NameReq>("""{"name":"foo"}""") shouldBe NameReq("foo")
  }
}
