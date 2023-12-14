package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class ZygardeApiPropProcessorTest {
  @Test
  fun `should able to generate model meta`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen-input/model-meta/Item.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      annotationProcessors = listOf(ZygardeApiPropProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    for (generatedFile in result.generatedFiles.filter { it.name.endsWith(".kt") }) {
      println(generatedFile.readText())
    }
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
  }
}
