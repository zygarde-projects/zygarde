package zygarde.data.option

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OptionEnumTest {
  enum class TestStatus(override val label: String) : OptionEnum {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
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
    dtos[1].key shouldBe "INACTIVE"
    dtos[1].label shouldBe "Inactive"
    dtos[2].key shouldBe "PENDING"
    dtos[2].label shouldBe "Pending"
  }
}
