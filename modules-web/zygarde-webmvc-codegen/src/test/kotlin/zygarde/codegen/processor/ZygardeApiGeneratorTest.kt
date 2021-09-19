package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions

class ZygardeApiGeneratorTest {

  @Test
  fun `should able to generate api`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("input/spec/AuthorApiSpec.kt").file,
        ClassPathResource("input/spec/BookApiSpec.kt").file,
        ClassPathResource("input/model/Author.kt").file,
        ClassPathResource("input/model/Book.kt").file,
        ClassPathResource("input/model/BookTagsValueProvider.kt").file,
        ClassPathResource("input/model/User.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      annotationProcessors = listOf(ZygardeApiProcessor(), ZygardeApiPropProcessor(), ZygardeJpaProcessor())
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
