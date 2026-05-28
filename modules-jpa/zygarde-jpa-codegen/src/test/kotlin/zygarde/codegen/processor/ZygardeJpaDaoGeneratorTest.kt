package zygarde.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeLessThan
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
      jvmTarget = JvmTarget.JVM_21.description
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
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
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
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val simpleBookDaoExtensions = result.generatedFiles.find { it.name == "SimpleBookDaoExtensions.kt" }!!.readText()
    simpleBookDaoExtensions shouldNotContain "fun SimpleBookDao.remove("
  }

  @Test
  fun `should generate scope data class for ScopeMarker entity`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }

    // Should generate scope data class
    generatedFileNames shouldContain "ScopedOrderScope.kt"

    val scopeContent = result.generatedFiles.first { it.name == "ScopedOrderScope.kt" }.readText()
    scopeContent shouldContain "data class ScopedOrderScope"
    scopeContent shouldContain "platformSource"

    // Extension functions should require scope parameter
    val extContent = result.generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: ScopedOrderScope"
    extContent shouldContain "fun ScopedOrderDao.search("
    extContent shouldContain "fun ScopedOrderDao.searchOne("
    extContent shouldContain "fun ScopedOrderDao.searchCount("
    extContent shouldContain "fun <PATCH> ScopedOrderDao.patchOne("
    extContent shouldContain "searchOneOrThrow(scope, errorCode, searchContent)"
  }

  @Test
  fun `should generate merged scope for entity implementing multiple ScopeMarker interfaces`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }

    generatedFileNames shouldContain "MultiScopedOrderScope.kt"

    val scopeContent = result.generatedFiles.first { it.name == "MultiScopedOrderScope.kt" }.readText()
    scopeContent shouldContain "data class MultiScopedOrderScope"
    scopeContent shouldContain "platformSource"
    scopeContent shouldContain "tenantId"
  }

  @Test
  fun `sorted overload should apply scope predicates`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = result.generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()

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
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = result.generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()

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
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = result.generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()

    // NullEquivalent("HOTCAKE") should generate: check for sentinel value and OR IS NULL
    extContent shouldContain "HOTCAKE"
    extContent shouldContain "isNull()"
    extContent shouldContain "or"
  }

  @Test
  fun `boolean scope property should be included in scope data class`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }

    generatedFileNames shouldContain "ToggleableItemScope.kt"

    val scopeContent = result.generatedFiles.first { it.name == "ToggleableItemScope.kt" }.readText()
    scopeContent shouldContain "data class ToggleableItemScope"
    scopeContent shouldContain "enabled"
    // Boolean type should be properly resolved
    scopeContent shouldContain "Boolean"

    // Extension functions should use scope
    val extContent = result.generatedFiles.first { it.name == "ToggleableItemDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: ToggleableItemScope"
    extContent shouldContain "\"enabled\""
  }

  @Test
  fun `scope data class should wrap field type with Collection and provide convenience constructor`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }

    generatedFileNames shouldContain "RegionalProductScope.kt"

    val scopeContent = result.generatedFiles.first { it.name == "RegionalProductScope.kt" }.readText()
    scopeContent shouldContain "data class RegionalProductScope"
    // Primary constructor should use Collection<Long>
    scopeContent shouldContain "Collection<Long>"
    // Convenience constructor should accept single Long value
    scopeContent shouldContain "listOf(regionId)"

    // Extension functions should use scope
    val extContent = result.generatedFiles.first { it.name == "RegionalProductDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: RegionalProductScope"
    extContent shouldContain "field<Long>(\"regionId\") inList scope.regionId"
  }

  @Test
  fun `searchOneOrThrow should include scope parameter and delegate to scoped searchOne`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = result.generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()

    extContent shouldContain "fun ScopedOrderDao.searchOneOrThrow("
    val searchOneOrThrowHead = extContent
      .substringAfter("fun ScopedOrderDao.searchOneOrThrow(")
      .substringBefore(")")
    searchOneOrThrowHead shouldContain "scope: ScopedOrderScope"
    searchOneOrThrowHead shouldContain "errorCode:"
    searchOneOrThrowHead.indexOf("scope: ScopedOrderScope") shouldBeLessThan searchOneOrThrowHead.indexOf("errorCode:")
    extContent shouldContain "searchOne(scope, searchContent)"
  }

  @Test
  fun `entity inheriting transitive ScopeMarker should still generate scope`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }

    generatedFileNames shouldContain "OwnedDocumentScope.kt"

    val scopeContent = result.generatedFiles.first { it.name == "OwnedDocumentScope.kt" }.readText()
    scopeContent shouldContain "data class OwnedDocumentScope"
    scopeContent shouldContain "ownerId"

    val extContent = result.generatedFiles.first { it.name == "OwnedDocumentDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: OwnedDocumentScope"
    extContent shouldContain "field<Long>(\"ownerId\") inList scope.ownerId"
  }

  @Test
  fun `empty ScopeMarker interface should fall back to unscoped extensions`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val generatedFileNames = result.generatedFiles.map { it.name }

    generatedFileNames shouldNotContain "UnscopedByEmptyMarkerScope.kt"

    val extContent = result.generatedFiles.first { it.name == "UnscopedByEmptyMarkerDaoExtensions.kt" }.readText()
    extContent shouldNotContain "scope: UnscopedByEmptyMarkerScope"
    extContent shouldContain "fun UnscopedByEmptyMarkerDao.search(searchContent:"
  }

  @Test
  fun `scoped entity with ZygardeEnhancedDao should generate scoped remove`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_INHERIT, "zygarde.data.jpa.dao.ZygardeEnhancedDao")
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_COMBINE, "false")
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    val extContent = result.generatedFiles.first { it.name == "ScopedOrderDaoExtensions.kt" }.readText()

    extContent shouldContain "fun ScopedOrderDao.remove("
    extContent shouldContain "scope: ScopedOrderScope"
    val removeHead = extContent
      .substringAfter("fun ScopedOrderDao.remove(")
      .substringBefore(")")
    removeHead shouldContain "scope: ScopedOrderScope"
    val removeBody = extContent.substringAfter("fun ScopedOrderDao.remove(")
    removeBody shouldContain "originalSearchContent"
    removeBody shouldContain "delete(SearchSpecBuilder.buildSpec(searchContent))"
  }

  @Test
  fun `should able to generate Dao with kaptOptions`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateDao.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_21.description
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
