package zygarde.codegen.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeLessThan
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
    simpleBookDaoExtensions shouldContain "fun <PATCH> SimpleBookDao.patchOne("
    simpleBookDaoExtensions shouldContain "patch: PATCH"
    simpleBookDaoExtensions shouldContain "patchContent: SimpleBook.(patch: PATCH) -> Unit"
    simpleBookDaoExtensions shouldContain "entity.patchContent(patch)"
    simpleBookDaoExtensions shouldNotContain "JsonNode"
    simpleBookDaoExtensions shouldNotContain "objectMapper"
    simpleBookDaoExtensions shouldNotContain "patch.has("
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
  fun `should generate scope data class for ScopeMarker entity`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
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

    // Should generate scope data class
    generatedFileNames shouldContain "ScopedOrderScope.kt"

    val scopeContent = generatedFiles.first { it.name == "ScopedOrderScope.kt" }.readText()
    scopeContent shouldContain "data class ScopedOrderScope"
    scopeContent shouldContain "platformSource"

    // Extension functions should require scope parameter
    val extContent = generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: ScopedOrderScope"
    extContent shouldContain "fun ScopedOrderDao.search("
    extContent shouldContain "fun ScopedOrderDao.searchOne("
    extContent shouldContain "fun ScopedOrderDao.searchCount("
    extContent shouldContain "fun <PATCH> ScopedOrderDao.patchOne("
    extContent shouldContain "searchOneOrThrow(scope, errorCode, searchContent)"
  }

  @Test
  fun `should generate merged scope for entity implementing multiple ScopeMarker interfaces`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
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

    generatedFileNames shouldContain "MultiScopedOrderScope.kt"

    val scopeContent = generatedFiles.first { it.name == "MultiScopedOrderScope.kt" }.readText()
    scopeContent shouldContain "data class MultiScopedOrderScope"
    scopeContent shouldContain "platformSource"
    scopeContent shouldContain "tenantId"
  }

  @Test
  fun `sorted overload should apply scope predicates`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "ScopedOrderDaoExtensions.kt" }!!.readText()

    // The sorted overload (sorts parameter) must wrap searchContent with scope predicates
    // It should contain originalSearchContent pattern, proving scope is applied before delegation
    extContent shouldContain "originalSearchContent"
    // Count occurrences: every scoped function that touches searchContent should wrap it
    val originalCount = "originalSearchContent".toRegex().findAll(extContent).count()
    // search(scope, default), search(scope, sorts, ...), search(scope, ..., limit),
    // searchCount, searchOne, searchPage = 6 functions that wrap, each uses it twice = 12
    // searchOneOrThrow delegates to searchOne so doesn't wrap
    originalCount shouldBe 12
  }

  @Test
  fun `sorted overload fallback should not re-delegate to scoped search`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "ScopedOrderDaoExtensions.kt" }!!.readText()

    // Isolate the sorted overload: `search(scope, sorts, searchContent)`.
    // Its sorts-null fallback must call findAll directly — delegating back
    // to `search(scope, searchContent)` would wrap the scope predicates a
    // second time.
    val sortedOverload = extContent
      .substringAfter("sorts: List<SortField>?")
      .substringBefore("public fun ScopedOrderDao.")
    sortedOverload shouldContain "?: findAll(SearchSpecBuilder.buildSpec(searchContent))"
    sortedOverload shouldNotContain "?: search(scope, searchContent)"
  }

  @Test
  fun `NullEquivalent should generate OR IS NULL logic`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "ScopedOrderDaoExtensions.kt" }!!.readText()

    // NullEquivalent("HOTCAKE") should generate: check for sentinel value and OR IS NULL
    extContent shouldContain "HOTCAKE"
    extContent shouldContain "isNull()"
    extContent shouldContain "or"
  }

  @Test
  fun `boolean scope property should be included in scope data class`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
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

    generatedFileNames shouldContain "ToggleableItemScope.kt"

    val scopeContent = generatedFiles.first { it.name == "ToggleableItemScope.kt" }.readText()
    scopeContent shouldContain "data class ToggleableItemScope"
    scopeContent shouldContain "enabled"
    // Boolean type should be properly resolved
    scopeContent shouldContain "Boolean"

    // Extension functions should use scope
    val extContent = generatedFiles.first { it.name == "ToggleableItemDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: ToggleableItemScope"
    extContent shouldContain "\"enabled\""
  }

  @Test
  fun `scope data class should wrap field type with Collection and provide convenience constructor`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
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

    generatedFileNames shouldContain "RegionalProductScope.kt"

    val scopeContent = generatedFiles.first { it.name == "RegionalProductScope.kt" }.readText()
    scopeContent shouldContain "data class RegionalProductScope"
    // Primary constructor should use Collection<Long>
    scopeContent shouldContain "Collection<Long>"
    // Convenience constructor should accept single Long value
    scopeContent shouldContain "listOf(regionId)"

    // Extension functions should use scope
    val extContent = generatedFiles.first { it.name == "RegionalProductDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: RegionalProductScope"
    extContent shouldContain "field<Long>(\"regionId\") inList scope.regionId"
  }

  @Test
  fun `searchOneOrThrow should include scope parameter and delegate to scoped searchOne`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "ScopedOrderDaoExtensions.kt" }!!.readText()

    extContent shouldContain "fun ScopedOrderDao.searchOneOrThrow("
    // signature must include scope before errorCode
    val searchOneOrThrowHead = extContent
      .substringAfter("fun ScopedOrderDao.searchOneOrThrow(")
      .substringBefore(")")
    searchOneOrThrowHead shouldContain "scope: ScopedOrderScope"
    searchOneOrThrowHead shouldContain "errorCode:"
    searchOneOrThrowHead.indexOf("scope: ScopedOrderScope") shouldBeLessThan searchOneOrThrowHead.indexOf("errorCode:")
    // Body delegates via searchOne(scope, searchContent) rather than wrapping itself
    extContent shouldContain "searchOne(scope, searchContent)"
  }

  @Test
  fun `entity inheriting transitive ScopeMarker should still generate scope`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
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

    generatedFileNames shouldContain "OwnedDocumentScope.kt"

    val scopeContent = generatedFiles.first { it.name == "OwnedDocumentScope.kt" }.readText()
    scopeContent shouldContain "data class OwnedDocumentScope"
    scopeContent shouldContain "ownerId"

    val extContent = generatedFiles.first { it.name == "OwnedDocumentDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: OwnedDocumentScope"
    extContent shouldContain "field<Long>(\"ownerId\") inList scope.ownerId"
  }

  @Test
  fun `empty ScopeMarker interface should fall back to unscoped extensions`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
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

    // No scope data class should be emitted for an empty scope marker
    generatedFileNames shouldNotContain "UnscopedByEmptyMarkerScope.kt"

    val extContent = generatedFiles.first { it.name == "UnscopedByEmptyMarkerDaoExtensions.kt" }.readText()
    extContent shouldNotContain "scope: UnscopedByEmptyMarkerScope"
    extContent shouldContain "fun UnscopedByEmptyMarkerDao.search(searchContent:"
  }

  @Test
  fun `scoped entity with ZygardeEnhancedDao should generate scoped remove`() {
    val compilation = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      symbolProcessorProviders = listOf(ZygardeJpaKspProcessorProvider())
      inheritClassPath = true
      messageOutputStream = System.out
      kspArgs[ZygardeJpaKspOptions.DAO_INHERIT] = "zygarde.data.jpa.dao.ZygardeEnhancedDao"
      kspArgs[ZygardeJpaKspOptions.DAO_COMBINE] = "false"
    }
    val result = compilation.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = compilation.kspSourcesDir.walkTopDown()
      .find { it.name == "ScopedOrderDaoExtensions.kt" }!!.readText()

    extContent shouldContain "fun ScopedOrderDao.remove("
    extContent shouldContain "scope: ScopedOrderScope"
    // remove signature takes scope first
    val removeHead = extContent
      .substringAfter("fun ScopedOrderDao.remove(")
      .substringBefore(")")
    removeHead shouldContain "scope: ScopedOrderScope"
    // remove body wraps searchContent with scope predicates and delegates to delete()
    val removeBody = extContent.substringAfter("fun ScopedOrderDao.remove(")
    removeBody shouldContain "originalSearchContent"
    removeBody shouldContain "delete(SearchSpecBuilder.buildSpec(searchContent))"
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
