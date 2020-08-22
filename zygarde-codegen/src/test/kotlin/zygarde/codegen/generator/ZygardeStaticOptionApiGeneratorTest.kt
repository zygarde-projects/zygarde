package zygarde.codegen.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.processor.ZygardeStaticOptionApiProcessor

class ZygardeStaticOptionApiGeneratorTest : StringSpec(
  {

    "should able to generate static option api" {
      val result = KotlinCompilation().apply {
        sources = listOf(
          ClassPathResource("codegen/options/BarType.kt").file,
          ClassPathResource("codegen/options/FooType.kt").file
        ).map { SourceFile.fromPath(it) }
        jvmTarget = JvmTarget.JVM_1_8.description
        annotationProcessors = listOf(ZygardeStaticOptionApiProcessor())
        inheritClassPath = true
        messageOutputStream = System.out
        kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
      }.compile()
      result.exitCode shouldBe KotlinCompilation.ExitCode.OK
      val generatedFileNames = result.generatedFiles.map { it.name }
      generatedFileNames shouldContain "StaticOptionApi.kt"

      result.generatedFiles.filter { it.absolutePath.endsWith("kt") }.forEach {
        println(it.absolutePath)
        println(it.readText())
      }
    }
  }
)
