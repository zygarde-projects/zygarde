package zygarde.data.option

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OptionDtoTest {
  @Test
  fun `should create OptionDto with required fields`() {
    // given & when
    val option =
      OptionDto(
        key = "active",
        label = "Active",
      )

    // then
    option.key shouldBe "active"
    option.label shouldBe "Active"
    option.active shouldBe true
  }

  @Test
  fun `should create OptionDto with all fields`() {
    // given & when
    val option =
      OptionDto(
        key = "inactive",
        label = "Inactive",
        active = false,
      )

    // then
    option.key shouldBe "inactive"
    option.label shouldBe "Inactive"
    option.active shouldBe false
  }
}
