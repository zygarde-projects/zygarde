package zygarde.codegen.dsl

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.codegen.meta.CodegenDtoSimple

class SealedInterfaceSpecTest {
  enum class TestDtos : CodegenDtoSimple {
    FooDto,
    BarDto,
  }

  @Test
  fun `should build sealed interface with default discriminator`() {
    val spec = SealedInterfaceSpec("MyResult")
    spec.subtype("foo", TestDtos.FooDto)
    spec.subtype("bar", TestDtos.BarDto)
    val result = spec.build()

    result.name shouldBe "MyResult"
    result.discriminatorProperty shouldBe "type"
    result.subtypes.size shouldBe 2
    result.subtypes[0].discriminatorValue shouldBe "foo"
    result.subtypes[0].dto shouldBe TestDtos.FooDto
    result.subtypes[1].discriminatorValue shouldBe "bar"
    result.subtypes[1].dto shouldBe TestDtos.BarDto
  }

  @Test
  fun `should build sealed interface with custom discriminator`() {
    val spec = SealedInterfaceSpec("MyResult")
    spec.discriminatorProperty = "kind"
    spec.subtype("a", TestDtos.FooDto)
    val result = spec.build()

    result.discriminatorProperty shouldBe "kind"
  }
}
