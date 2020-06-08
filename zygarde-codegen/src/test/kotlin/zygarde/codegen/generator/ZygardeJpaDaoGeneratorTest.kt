package zygarde.codegen.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.config.JvmTarget
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.processor.ZygardeProcessor

class ZygardeJpaDaoGeneratorTest : StringSpec({

  "should able to generate Dao" {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      annotationProcessors = listOf(ZygardeProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }
    generatedFileNames shouldContain "SimpleBookDao.kt"
    generatedFileNames shouldContain "AutoIntIdBookDao.kt"
    generatedFileNames shouldContain "AutoLongIdBookDao.kt"
    generatedFileNames shouldContain "IdClassBookDao.kt"
    generatedFileNames shouldContain "Dao.kt"

    result.generatedFiles.filter { it.absolutePath.endsWith("kt") }.forEach {
      println(it.absolutePath)
      println(it.readText())
    }
  }

  "should able to generate Dao with kaptOptions" {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      annotationProcessors = listOf(ZygardeProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
      kaptArgs.put(ZygardeKaptOptions.DAO_PACKAGE, "daos")
      kaptArgs.put(ZygardeKaptOptions.DAO_SUFFIX, "BaseDao")
      kaptArgs.put(ZygardeKaptOptions.DAO_COMBINE, "false")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }
    generatedFileNames shouldContain "SimpleBookBaseDao.kt"
    generatedFileNames shouldContain "AutoIntIdBookBaseDao.kt"
    generatedFileNames shouldContain "AutoLongIdBookBaseDao.kt"
    generatedFileNames shouldContain "IdClassBookBaseDao.kt"
    generatedFileNames shouldNotContain "Dao.kt"

    result.generatedFiles.filter { it.absolutePath.endsWith("kt") }.forEach {
      println(it.absolutePath)
      println(it.readText())
    }
  }
})
