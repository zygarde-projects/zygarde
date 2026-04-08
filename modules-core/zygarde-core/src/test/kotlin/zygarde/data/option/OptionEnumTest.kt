package zygarde.data.option

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OptionEnumTest {
  enum class TestStatus(
    override val label: String,
    override val active: Boolean = true
  ) : OptionEnum {
    ACTIVE("Active"),
    INACTIVE("Inactive", active = false),
    PENDING("Pending")
  }

  @Test
  fun `should convert enum to OptionDto`() {
    // given
    val status = TestStatus.ACTIVE

    // when
    val dto = status.toOptionDto()

    // then
    dto.key shouldBe "ACTIVE"
    dto.label shouldBe "Active"
    dto.active shouldBe true
  }

  @Test
  fun `should convert all enum values to OptionDto`() {
    // given
    val statuses = TestStatus.values()

    // when
    val dtos = statuses.map { it.toOptionDto() }

    // then
    dtos.size shouldBe 3
    dtos[0].key shouldBe "ACTIVE"
    dtos[0].label shouldBe "Active"
    dtos[0].active shouldBe true
    dtos[1].key shouldBe "INACTIVE"
    dtos[1].label shouldBe "Inactive"
    dtos[1].active shouldBe false
    dtos[2].key shouldBe "PENDING"
    dtos[2].label shouldBe "Pending"
    dtos[2].active shouldBe true
  }

  @Test
  fun `should override active with activeOverrides map`() {
    // given
    val activeOverrides = mapOf("ACTIVE" to false, "INACTIVE" to true)

    // when
    val dtos = TestStatus.values().map { it.toOptionDto(activeOverrides) }

    // then
    dtos[0].key shouldBe "ACTIVE"
    dtos[0].active shouldBe false
    dtos[1].key shouldBe "INACTIVE"
    dtos[1].active shouldBe true
    dtos[2].key shouldBe "PENDING"
    dtos[2].active shouldBe true
  }

  @Test
  fun `should fallback to enum active when activeOverrides is null`() {
    // given / when
    val dto = TestStatus.INACTIVE.toOptionDto(null)

    // then
    dto.active shouldBe false
  }

  @Test
  fun `should use enum defaults when props active map is empty`() {
    // simulates: zygardeApiProperties.staticOptionApi.active = emptyMap()
    val propsActive: Map<String, Map<String, Boolean>> = emptyMap()

    // when - same lookup pattern as generated controller
    val dtos = TestStatus.values().map { it.toOptionDto(propsActive["TestStatus"]) }

    // then - all values follow enum-defined active
    dtos[0].key shouldBe "ACTIVE"
    dtos[0].active shouldBe true
    dtos[1].key shouldBe "INACTIVE"
    dtos[1].active shouldBe false
    dtos[2].key shouldBe "PENDING"
    dtos[2].active shouldBe true
  }

  @Test
  fun `should override active when props active map has entries for enum type`() {
    // simulates: zygarde.api.static-option-api.active.TestStatus.INACTIVE=true
    val propsActive: Map<String, Map<String, Boolean>> = mapOf(
      "TestStatus" to mapOf("INACTIVE" to true)
    )

    // when
    val dtos = TestStatus.values().map { it.toOptionDto(propsActive["TestStatus"]) }

    // then - INACTIVE overridden to true, others follow enum defaults
    dtos[0].key shouldBe "ACTIVE"
    dtos[0].active shouldBe true
    dtos[1].key shouldBe "INACTIVE"
    dtos[1].active shouldBe true
    dtos[2].key shouldBe "PENDING"
    dtos[2].active shouldBe true
  }

  @Test
  fun `should not affect enum type when props active map has entries for different enum`() {
    // simulates: only OtherEnum is configured, not TestStatus
    val propsActive: Map<String, Map<String, Boolean>> = mapOf(
      "OtherEnum" to mapOf("ACTIVE" to false)
    )

    // when
    val dtos = TestStatus.values().map { it.toOptionDto(propsActive["TestStatus"]) }

    // then - no override for TestStatus, all follow enum defaults
    dtos[0].active shouldBe true
    dtos[1].active shouldBe false
    dtos[2].active shouldBe true
  }
}
