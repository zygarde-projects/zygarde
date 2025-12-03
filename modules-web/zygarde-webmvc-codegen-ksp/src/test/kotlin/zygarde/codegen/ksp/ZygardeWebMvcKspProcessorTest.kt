package zygarde.codegen.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

@OptIn(ExperimentalCompilerApi::class)
class ZygardeApiKspProcessorTest {
  @Test
  fun `should able to generate API interface and controller`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/TestApiSpec.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeApiKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val generatedFileNames = compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.extension == "kt" }
      .map { f -> f.name }
      .toList()

    generatedFileNames shouldContain "TestApi.kt"
    generatedFileNames shouldContain "TestApiFeign.kt"
    generatedFileNames shouldContain "TestApiController.kt"
    generatedFileNames shouldContain "TestService.kt"
  }
}

@OptIn(ExperimentalCompilerApi::class)
class ZygardeStaticOptionApiKspProcessorTest {
  @Test
  fun `should able to generate static option API`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/TestStatus.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeStaticOptionApiKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val generatedFileNames = compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.extension == "kt" }
      .map { f -> f.name }
      .toList()

    generatedFileNames shouldContain "StaticOptionDto.kt"
    generatedFileNames shouldContain "StaticOptionApi.kt"
    generatedFileNames shouldContain "StaticOptionController.kt"

    val dtoFile = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "StaticOptionDto.kt" }
    dtoFile?.readText() shouldContain "testStatus"
  }
}
