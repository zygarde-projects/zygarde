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
    // combined Dao must use field injection — a generated constructor hits the JVM 255-param
    // ceiling once the schema grows to ~250 DAOs
    val combinedDao = result.generatedFiles.find { it.name == "Dao.kt" }!!.readText()
    combinedDao shouldContain "lateinit var simpleBookDao: SimpleBookDao"
    combinedDao shouldContain "@Autowired"
    combinedDao shouldNotContain "public class Dao("
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
    simpleBookDaoExtensions shouldNotContain "ScopeFilter"
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
    extContent shouldContain "level = DeprecationLevel.ERROR"
    extContent shouldContain "請以第一參數傳入 ScopedOrderScope"
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
  fun `ScopeOp should generate selected predicates and Boolean null checks`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val scopeContent = result.generatedFiles.first { it.name == "AdvancedScopedOrderScope.kt" }.readText()
    scopeContent shouldContain "val status: ScopeFilter<String> = ScopeFilter.All"
    scopeContent shouldContain "status: Collection<String>?,"
    scopeContent shouldContain "deletedAt: Boolean? = null"
    scopeContent shouldContain "linkedId: Boolean? = null"

    val extContent = result.generatedFiles.first { it.name == "AdvancedScopedOrderDaoExtensions.kt" }.readText()
    extContent shouldContain "field<String>(\"status\") notInList scopeValues"
    extContent shouldContain "scopeValues.any { it.toString() == \"ARCHIVED\" }"
    extContent shouldContain "field<String>(\"deletedAt\").isNull()"
    extContent shouldContain "field<Long>(\"linkedId\").isNotNull()"
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
  fun `scope data class should use ScopeFilter and preserve collection and single-value constructors`() {
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
    // Primary constructor distinguishes All from Of(values).
    scopeContent shouldContain "val regionId: ScopeFilter<Long>"
    // Compatibility constructor should continue accepting Collection<Long>.
    scopeContent shouldContain "Collection<Long>"
    // Convenience constructor should accept single Long value
    scopeContent shouldContain "ScopeFilter.Of(listOf(regionId))"

    // Extension functions should use scope
    val extContent = result.generatedFiles.first { it.name == "RegionalProductDaoExtensions.kt" }.readText()
    extContent shouldContain "scope: RegionalProductScope"
    extContent shouldContain "ScopeFilter.All -> Unit"
    extContent shouldContain "val scopeValues = scopeFilter.values"
    extContent shouldContain "field<Long>(\"regionId\") inList scopeValues"
  }

  @Test
  fun `nullable scope constructors should resolve null without ambiguity`() {
    val fixture = ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
    val generated = KotlinCompilation().apply {
      sources = listOf(SourceFile.fromPath(fixture))
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
    }.compile()
    generated.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val result = KotlinCompilation().apply {
      sources = listOf(SourceFile.fromPath(fixture)) +
        generated.generatedFiles
          .filter { it.name == "ScopedOrderScope.kt" || it.name == "AdvancedScopedOrderScope.kt" }
          .map(SourceFile::fromPath) +
        SourceFile.kotlin(
          "ScopeConstructorUsage.kt",
          """
          package codegen.jpa

          import zygarde.data.jpa.search.ScopeFilter
          import zygarde.generated.data.dao.AdvancedScopedOrderScope
          import zygarde.generated.data.dao.ScopedOrderScope

          fun scopeConstructors() {
            ScopedOrderScope()
            ScopedOrderScope(platformSource = null)
            ScopedOrderScope(null)
            ScopedOrderScope(platformSource = ScopeFilter.All)
            ScopedOrderScope(platformSource = emptyList())
            ScopedOrderScope(platformSource = "HOTCAKE")
            AdvancedScopedOrderScope()
            AdvancedScopedOrderScope(status = null)
            AdvancedScopedOrderScope(deletedAt = true)
          }
          """.trimIndent(),
        )
      jvmTarget = JvmTarget.JVM_17.description
      inheritClassPath = true
    }.compile()

    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    result.messages shouldNotContain "Overload resolution ambiguity"
  }

  @Test
  fun `scope data class should avoid constructors with the same erased JVM signature`() {
    val result = KotlinCompilation().apply {
      sources = listOf(
        ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
      ).map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val scopeContent = result.generatedFiles.first { it.name == "FilterValueEntityScope.kt" }.readText()
    scopeContent shouldContain "val filter: ScopeFilter<ScopeFilter<String>>"
    scopeContent shouldContain "filter: Collection<ScopeFilter<String>>"
    scopeContent shouldNotContain "constructor(filter: ScopeFilter<String>)"

    val collectionScopeContent = result.generatedFiles.first { it.name == "CollectionValueEntityScope.kt" }.readText()
    collectionScopeContent shouldNotContain "constructor(values: Collection<String>)"
    collectionScopeContent shouldNotContain "constructor(values: java.util.Collection<String>)"
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
    extContent shouldContain "field<Long>(\"ownerId\") inList scopeValues"
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
    val removeFunctions = extContent.split("fun ScopedOrderDao.remove(").drop(1)
    removeFunctions.size shouldBe 2
    removeFunctions.forEach { removeFunction ->
      removeFunction shouldContain "): Long"
    }
    val removeHead = extContent
      .substringAfter("fun ScopedOrderDao.remove(")
      .substringBefore(")")
    removeHead shouldContain "scope: ScopedOrderScope"
    val removeBody = extContent.substringAfter("fun ScopedOrderDao.remove(")
    removeBody shouldContain "originalSearchContent"
    removeBody shouldContain "delete(SearchSpecBuilder.buildSpec(searchContent))"
  }

  @Test
  fun `missing scope should report an actionable compiler error`() {
    val fixture = ClassPathResource("codegen/jpa/TestGenerateScopedQuery.kt").file
    val generated = KotlinCompilation().apply {
      sources = listOf(SourceFile.fromPath(fixture))
      jvmTarget = JvmTarget.JVM_17.description
      annotationProcessors = listOf(ZygardeJpaProcessor())
      inheritClassPath = true
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_INHERIT, "zygarde.data.jpa.dao.ZygardeEnhancedDao")
      kaptArgs.put(ZygardeJpaCodegenKaptOptions.DAO_COMBINE, "false")
    }.compile()
    generated.exitCode shouldBe KotlinCompilation.ExitCode.OK

    val requiredGeneratedFiles = setOf(
      "ScopedOrderDao.kt",
      "ScopedOrderScope.kt",
      "ScopedOrderDaoExtensions.kt",
      "AdvancedScopedOrderScope.kt",
    )
    val result = KotlinCompilation().apply {
      sources = listOf(SourceFile.fromPath(fixture)) +
        generated.generatedFiles
          .filter { it.name in requiredGeneratedFiles }
          .map(SourceFile::fromPath) +
        SourceFile.kotlin(
          "MissingScopeUsage.kt",
          """
          package codegen.jpa

          import zygarde.generated.data.dao.ScopedOrderDao
          import zygarde.generated.data.dao.AdvancedScopedOrderScope
          import zygarde.core.exception.CommonErrorCode
          import zygarde.data.api.PagingAndSortingRequest
          import zygarde.generated.data.dao.search
          import zygarde.generated.data.dao.searchCount
          import zygarde.generated.data.dao.searchOne
          import zygarde.generated.data.dao.searchOneOrThrow
          import zygarde.generated.data.dao.searchPage
          import zygarde.generated.data.dao.remove

          fun missingScope(dao: ScopedOrderDao) {
            AdvancedScopedOrderScope()
            AdvancedScopedOrderScope(deletedAt = true)
            dao.search { field<String>("platformSource") eq "HOTCAKE" }
            dao.search(sorts = null) { field<String>("platformSource") eq "HOTCAKE" }
            dao.search(searchContent = { field<String>("platformSource") eq "HOTCAKE" }, limit = 1)
            dao.searchCount { field<String>("platformSource") eq "HOTCAKE" }
            dao.searchOne { field<String>("platformSource") eq "HOTCAKE" }
            dao.searchOneOrThrow(CommonErrorCode.ERROR) { field<String>("platformSource") eq "HOTCAKE" }
            dao.searchPage(PagingAndSortingRequest()) { field<String>("platformSource") eq "HOTCAKE" }
            dao.remove { field<String>("platformSource") eq "HOTCAKE" }
          }
          """.trimIndent(),
        )
      jvmTarget = JvmTarget.JVM_17.description
      inheritClassPath = true
    }.compile()

    result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    result.messages shouldContain "請以第一參數傳入 ScopedOrderScope"
    result.messages shouldContain "ScopedOrderScope(platformSource = ScopeFilter.All)"
    result.messages shouldNotContain "Overload resolution ambiguity"
    result.messages shouldNotContain "Unresolved reference: field"
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
