package zygarde.codegen.dsl

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class ModelMappingSortableFieldsCompilationTest {
  @Test
  fun `should reject mismatched sortable path at compile time`() {
    compileInvalidPath("spec.sortableField(Book::author, Publisher::name)") shouldBe
      KotlinCompilation.ExitCode.COMPILATION_ERROR
  }

  @Test
  fun `should reject to-many sortable path at compile time`() {
    compileInvalidPath("spec.sortableField(Book::authors, Author::name)") shouldBe
      KotlinCompilation.ExitCode.COMPILATION_ERROR
  }

  private fun compileInvalidPath(statement: String): KotlinCompilation.ExitCode {
    return KotlinCompilation().apply {
      inheritClassPath = true
      messageOutputStream = System.out
      sources = listOf(
        SourceFile.kotlin(
          "InvalidSortablePaths.kt",
          """
          package compiletest

          import zygarde.codegen.dsl.ModelMappingSpec

          class Author(val name: String)
          class Publisher(val name: String)
          class Book(
            val author: Author?,
            val authors: List<Author>,
          )

          fun invalid(spec: ModelMappingSpec) {
            $statement
          }
          """.trimIndent()
        )
      )
    }.compile().exitCode
  }
}
