package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
      it shouldContain "var barType"
      it shouldContain "var fooType"
      // constructor defaults would blow the JVM 64KB method limit at scale
      it shouldNotContain "public data class"
    }
    generatedSources.getValue("StaticOptionApi.kt").readText().also {
      it shouldContain """value=["\${'$'}{zygarde.api.static-option-api.path}/barType"]"""
      it shouldContain """value=["\${'$'}{zygarde.api.static-option-api.path}/foo-types"]"""
      it shouldContain "fun getFooType()"
    }
    generatedSources.getValue("StaticOptionController.kt").readText().also {
      it shouldContain """activeOverrides["BarType"]"""
      it shouldContain """activeOverrides["foo-type"]"""
      it shouldContain "fillStaticOptions0(dto)"
    }
  }

  @Test
  fun `should scale to hundreds of annotated enums`() {
    // Regression guard: constructor-default generation failed with "Method too large"
    // once the annotated enum count reached ~159.
    val manyEnums = (1..300).joinToString("\n") { i ->
      """
      @zygarde.codegen.StaticOptionApi(comment = "Scale$i")
      enum class ScaleType$i(override val label: String) : zygarde.data.option.OptionEnum {
        A("a"), B("b"), C("c")
      }
      """.trimIndent()
    }
    val result = KotlinCompilation().apply {
      sources = listOf(SourceFile.kotlin("ScaleTypes.kt", "package foo.scale\n\n$manyEnums"))
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeStaticOptionApiProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val controllerSource = result.generatedFiles
      .filter { it.absolutePath.endsWith("kt") }
      .find { it.name == "StaticOptionController.kt" }!!.readText()
    controllerSource shouldContain "fillStaticOptions5(dto)"
  }
}
