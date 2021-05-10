package zygarde.codegen.generator

import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.CodegenDtoSimple
import zygarde.codegen.dsl.codegen

class CodegenDslTest {

  enum class TestDtos : CodegenDtoSimple {
    FooDto,
    BarDto,
  }

  @Test
  fun `should able to codegen using dsl`() {
    codegen {
      dto<TestDtos>()
      dtoFields(TestDtos.FooDto, TestDtos.BarDto) {
        extraField<Int>("id", "id")
      }
      dtoFields(TestDtos.FooDto) {
        extraField<String>("foo", "this is comment")
      }
      dtoFields(TestDtos.BarDto) {
        extraField<String>("bar", "nullable", true)
      }
    }
  }
}
