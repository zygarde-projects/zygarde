package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.codegen.ZygardeJpaCodegenKaptOptions
import zygarde.codegen.ZygardeKaptOptions

@OptIn(ExperimentalCompilerApi::class)
class ZygardeJpaDaoGeneratorTest {
  @Test
  fun `should able to generate Dao`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }
    generatedFileNames shouldContain "SimpleBookDao.kt"
    generatedFileNames shouldContain "AutoIntIdBookDao.kt"
    generatedFileNames shouldContain "AutoLongIdBookDao.kt"
    generatedFileNames shouldContain "AuditedAutoIntIdBookDao.kt"
    generatedFileNames shouldContain "SequenceAutoIntIdBookDao.kt"
    generatedFileNames shouldContain "IdClassBookDao.kt"
    generatedFileNames shouldContain "Dao.kt"
    generatedFileNames shouldContain "SimpleBookDaoExtensions.kt"
    generatedFileNames shouldContain "AutoIntIdBookDaoExtensions.kt"
    generatedFileNames shouldContain "AutoLongIdBookDaoExtensions.kt"
    val simpleBookDaoExtensions = result.generatedFiles.find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.search("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchOne("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchCount("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchPage("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchOneOrThrow("
  }

  @Test
  fun `should able to generate Enhanced Dao`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_INHERIT, "zygarde.data.jpa.dao.ZygardeEnhancedDao")
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_COMBINE, "false")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    result.generatedFiles.filter { it.name.endsWith("Dao") }.forEach {
      it.readText() shouldContain "ZygardeEnhancedDao"
    }
    val simpleBookDaoExtensions = result.generatedFiles.find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.remove("
  }

  @Test
  fun `should not generate remove when not using ZygardeEnhancedDao`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val simpleBookDaoExtensions = result.generatedFiles.find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldNotContain "fun SimpleBookDao.remove("
  }

  @Test
  fun `should able to generate Dao with kaptOptions`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeKaptOptions.BASE_PACKAGE, "foo.generated")
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_PACKAGE, "daos")
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_SUFFIX, "BaseDao")
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_COMBINE, "false")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }
    generatedFileNames shouldContain "SimpleBookBaseDao.kt"
    generatedFileNames shouldContain "AutoIntIdBookBaseDao.kt"
    generatedFileNames shouldContain "AutoLongIdBookBaseDao.kt"
    generatedFileNames shouldContain "IdClassBookBaseDao.kt"
    generatedFileNames shouldNotContain "Dao.kt"
  }
}
