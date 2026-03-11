package zygarde.codegen.dsl.generator

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.CodegenSealedInterface
import zygarde.codegen.meta.SealedSubtypeMapping

class DtoFieldMappingCodeGeneratorSealedInterfaceTest {
  enum class TestDtos : CodegenDtoSimple {
    SuccessDto,
    FailureDto,
  }

  @Test
  fun `should generate sealed interface with Jackson and Schema annotations`() {
    val sealed = CodegenSealedInterface(
      name = "PaymentResult",
      discriminatorProperty = "type",
      subtypes = listOf(
        SealedSubtypeMapping("success", TestDtos.SuccessDto),
        SealedSubtypeMapping("failure", TestDtos.FailureDto),
      ),
    )

    val result = DtoFieldMappingCodeGenerator(
      dtoFieldMappings = emptyList(),
      sealedInterfaces = listOf(sealed),
    ).generateFileSpec()

    val sealedFiles = result.dtoFileSpecs.filter { it.name == "PaymentResult" }
    sealedFiles shouldHaveSize 1

    val output = sealedFiles.first().toString()
    output shouldContain "sealed interface PaymentResult"
    output shouldContain "@JsonTypeInfo"
    output shouldContain "JsonTypeInfo.Id.NAME"
    output shouldContain """property = "type""""
    output shouldContain "@JsonSubTypes"
    output shouldContain """JsonSubTypes.Type(value = SuccessDto::class, name = "success")"""
    output shouldContain """JsonSubTypes.Type(value = FailureDto::class, name = "failure")"""
    output shouldContain "@Schema"
    output shouldContain "oneOf = [SuccessDto::class, FailureDto::class]"
    output shouldContain """discriminatorProperty = "type""""
  }

  @Test
  fun `should add sealed interface as superinterface on subtype DTOs`() {
    val sealed = CodegenSealedInterface(
      name = "PaymentResult",
      discriminatorProperty = "type",
      subtypes = listOf(
        SealedSubtypeMapping("success", TestDtos.SuccessDto),
      ),
    )

    val generator = DtoFieldMappingCodeGenerator(
      dtoFieldMappings = emptyList(),
      sealedInterfaces = listOf(sealed),
    )

    generator.dtoToSealedInterface["SuccessDto"] shouldBe sealed
    generator.dtoToSealedInterface["FailureDto"] shouldBe null
  }
}
