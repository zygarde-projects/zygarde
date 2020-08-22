package zygarde.codegen.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.processor.ZygardeApiProcessor

class ZygardeApiGeneratorTest : StringSpec(
  {

    "should able to generate api" {
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
        annotationProcessors = listOf(ZygardeApiProcessor())
        inheritClassPath = true
        messageOutputStream = System.out
        kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
      }.compile()
      result.exitCode shouldBe KotlinCompilation.ExitCode.OK
      val generatedFileNames = result.generatedFiles.map { it.name }
      result.generatedFiles.filter { it.absolutePath.endsWith("kt") }.forEach {
        println(it.absolutePath)
        println(it.readText())
      }
    }
  }
)
