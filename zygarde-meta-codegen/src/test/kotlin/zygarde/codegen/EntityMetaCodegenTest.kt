package zygarde.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.processor.EntityMetaGenerateProcessor
import java.nio.file.Files

class EntityMetaCodegenTest {
  @Test
  fun `should able to generate entity meta`() {
    val tempBuildSrc = Files.createTempDirectory("buildSrc")
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("sample/SampleEntity.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      annotationProcessors = listOf(EntityMetaGenerateProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs[ZygardeKaptOptions.META_GEN_TARGET] = tempBuildSrc.toFile().absolutePath
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    result.generatedFiles.filter { it.absolutePath.endsWith("kt") }.forEach {
      println(it.absolutePath)
      println(it.readText())
    }

    FileUtils.listFiles(tempBuildSrc.toFile(), null, true).forEach {
      println(it.absolutePath)
      println(it.readText())
    }
  }
}
