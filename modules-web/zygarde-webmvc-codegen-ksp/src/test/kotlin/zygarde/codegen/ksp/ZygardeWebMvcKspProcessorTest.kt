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

    val controllerFile = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "TestApiController.kt" }
    val controllerSource = controllerFile?.readText().orEmpty()
    controllerSource shouldContain "@PatchMapping("
    controllerSource shouldContain """value=["/api/test/{id}"]"""
    controllerSource shouldContain """consumes=["application/merge-patch+json"]"""
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
    val dtoSource = dtoFile?.readText().orEmpty()
    dtoSource shouldContain "testStatus"
    dtoSource shouldContain "todoPriority"

    val apiFile = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "StaticOptionApi.kt" }
    val apiSource = apiFile?.readText().orEmpty()
    apiSource shouldContain """value=["\${'$'}{zygarde.api.static-option-api.path}/testStatus"]"""
    apiSource shouldContain """value=["\${'$'}{zygarde.api.static-option-api.path}/priorities"]"""
    apiSource shouldContain "fun getTodoPriority()"

    val controllerFile = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "StaticOptionController.kt" }
    val controllerSource = controllerFile?.readText().orEmpty()
    controllerSource shouldContain """activeOverrides["TestStatus"]"""
    controllerSource shouldContain """activeOverrides["todo-priority"]"""
    controllerSource shouldContain "fillStaticOptions0(dto)"
  }
}
