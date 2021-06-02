package zygarde.codegen.generator

import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.CodegenDsl
import zygarde.codegen.dsl.CodegenDtoSimple
import zygarde.codegen.dsl.EntityFieldDsl
import zygarde.codegen.dsl.codegen
import zygarde.codegen.generator.CodegenDslTest.TestDtos.*

class CodegenDslTest {

  class FooEntity(
    var name: String
  )

  class FooEntityMeta(val codegen: Codegen) {
    fun name(comment: String = "", dsl: EntityFieldDsl.() -> Unit) =
      dsl.invoke(EntityFieldDsl(FooEntity::class.java.canonicalName, codegen, "name", "java.lang.String", comment))
  }

  enum class TestDtos : CodegenDtoSimple {
    FooDto,
    BarDto,
  }

  fun CodegenDsl.FooEntity(dsl: FooEntityMeta.() -> Unit) = dsl.invoke(FooEntityMeta(codegen))

  @Test
  fun `should able to codegen using dsl`() {
    codegen {
      dtos<TestDtos>()
      dto(FooDto, BarDto) {
        extraField<Int>("id", "id")
      }
      dto(FooDto) {
        extraField<String>("foo", "this is comment")
      }
      dto(BarDto) {
        extraField<String>("bar", "nullable", true)
      }

      FooEntity {
        name(comment = "Name") {
          toDto(FooDto, BarDto)
        }
      }
    }
  }
}
