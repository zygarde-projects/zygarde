package zygarde.codegen.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

@OptIn(ExperimentalCompilerApi::class)
class ZygardeModelMappingKspProcessorTest {
  @Test
  fun `should able to generate DTO and extensions`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/TestItem.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeModelMappingKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val generatedFileNames = compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.extension == "kt" }
      .map { f -> f.name }
      .toList()

    generatedFileNames shouldContain "ItemDto.kt"
    generatedFileNames shouldContain "CreateItemReq.kt"
    generatedFileNames shouldContain "ItemDtoExtensions.kt"
  }

  @Test
  fun `should able to generate DTO with custom package`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/TestItem.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeModelMappingKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
      kspArgs[ZygardeModelMappingKspOptions.BASE_PACKAGE] = "custom.generated"
      kspArgs[ZygardeModelMappingKspOptions.MODEL_MAPPING_DTO_PACKAGE] = "dto"
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val generatedFiles = compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.extension == "kt" }
      .toList()

    generatedFiles.any { it.readText().contains("package custom.generated.dto") } shouldBe true
  }
}
