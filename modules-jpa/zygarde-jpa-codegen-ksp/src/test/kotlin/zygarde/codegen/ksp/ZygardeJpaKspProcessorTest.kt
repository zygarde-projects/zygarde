package zygarde.codegen.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

@OptIn(ExperimentalCompilerApi::class)
class ZygardeJpaKspProcessorTest {
  @Test
  fun `should able to generate Dao`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFiles = compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.extension == "kt" }
      .toList()
    val generatedFileNames = generatedFiles.map { it.name }
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
    val simpleBookDaoExtensions = generatedFiles.find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.search("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchOne("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchCount("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchPage("
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.searchOneOrThrow("
  }

  @Test
  fun `should able to generate Enhanced Dao`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
      kspArgs[ZygardeJpaKspOptions.DAO_INHERIT] = "zygarde.data.jpa.dao.ZygardeEnhancedDao"
      kspArgs[ZygardeJpaKspOptions.DAO_COMBINE] = "false"
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.name.endsWith("Dao.kt") }
      .forEach { f ->
        f.readText() shouldContain "ZygardeEnhancedDao"
      }
    val simpleBookDaoExtensions = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldContain "fun SimpleBookDao.remove("
  }

  @Test
  fun `should not generate remove when not using ZygardeEnhancedDao`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val simpleBookDaoExtensions = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldNotContain "fun SimpleBookDao.remove("
  }

  @Test
  fun `should able to generate Dao with kspOptions`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
      kspArgs[ZygardeJpaKspOptions.BASE_PACKAGE] = "foo.generated"
      kspArgs[ZygardeJpaKspOptions.DAO_PACKAGE] = "daos"
      kspArgs[ZygardeJpaKspOptions.DAO_SUFFIX] = "BaseDao"
      kspArgs[ZygardeJpaKspOptions.DAO_COMBINE] = "false"
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = compilation.kspSourcesDir.walkTopDown()
      .filter { f -> f.isFile && f.extension == "kt" }
      .map { f -> f.name }
      .toList()
    generatedFileNames shouldContain "SimpleBookBaseDao.kt"
    generatedFileNames shouldContain "AutoIntIdBookBaseDao.kt"
    generatedFileNames shouldContain "AutoLongIdBookBaseDao.kt"
    generatedFileNames shouldContain "IdClassBookBaseDao.kt"
    generatedFileNames shouldNotContain "Dao.kt"
  }
}
