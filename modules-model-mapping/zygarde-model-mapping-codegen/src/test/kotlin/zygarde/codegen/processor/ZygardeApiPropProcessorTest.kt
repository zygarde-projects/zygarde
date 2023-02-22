package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions
import java.nio.file.Files

class ZygardeApiPropProcessorTest {
  @Test
  fun `should able to generate model meta`() {
    val tempDirectory = Files.createTempDirectory("model-mapping").toFile()
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen-input/model-meta/Item.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeApiPropProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeKaptOptions.MODEL_META_GENERATE_TARGET, tempDirectory.absolutePath)
    }.compile()
    for (generatedFile in result.generatedFiles) {
      println(generatedFile.readText())
    }
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
  }
}
