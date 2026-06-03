package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions

@OptIn(ExperimentalCompilerApi::class)
class ZygardeStaticOptionApiProcessorTest {
  @Test
  fun `should able to generate static option api`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("input/options/BarType.kt").file,
        ClassPathResource("input/options/FooType.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeStaticOptionApiProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }
    generatedFileNames shouldContain "StaticOptionApi.kt"

    val generatedSources = result.generatedFiles
      .filter { it.absolutePath.endsWith("kt") }
      .associateBy { it.name }

    generatedSources.getValue("StaticOptionDto.kt").readText().also {
      it shouldContain "val barType"
      it shouldContain "val fooType"
    }
    generatedSources.getValue("StaticOptionApi.kt").readText().also {
      it shouldContain """value=["\${'$'}{zygarde.api.static-option-api.path}/barType"]"""
      it shouldContain """value=["\${'$'}{zygarde.api.static-option-api.path}/foo-types"]"""
      it shouldContain "fun getFooType()"
    }
    generatedSources.getValue("StaticOptionController.kt").readText().also {
      it shouldContain """activeOverrides["BarType"]"""
      it shouldContain """activeOverrides["foo-type"]"""
    }
  }
}
