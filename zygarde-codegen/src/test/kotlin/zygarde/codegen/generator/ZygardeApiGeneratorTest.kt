package zygarde.codegen.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.processor.ZygardeApiProcessor
import zygarde.codegen.processor.ZygardeJpaProcessor

class ZygardeApiGeneratorTest {

  @Test
  fun `should able to generate api`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/spec/AuthorApiSpec.kt").file,
        ClassPathResource("codegen/spec/BookApiSpec.kt").file,
        ClassPathResource("codegen/entity/Author.kt").file,
        ClassPathResource("codegen/entity/Book.kt").file,
        ClassPathResource("codegen/entity/BookTagsValueProvider.kt").file,
        ClassPathResource("codegen/entity/User.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      annotationProcessors = listOf(ZygardeJpaProcessor(), ZygardeApiProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    result.generatedFiles.filter { it.absolutePath.endsWith("kt") }.forEach {
      println(it.absolutePath)
      println(it.readText())
    }
  }
}
