package zygarde.codegen.generator

import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.DtoDsl
import zygarde.codegen.dsl.codegen

class CodegenDslTest {

  enum class TestDtos(
    vararg superClasses: String,
    private val superClassesArray: Array<out String> = superClasses
  ) : DtoDsl {
    FooDto,
    BarDto, ;

    override fun superClasses(): Array<out String> {
      return this.superClassesArray
    }
  }

  @Test
  fun `should able to codegen using dsl`() {
    codegen {
      dto(*TestDtos.values())
    }
  }
}
