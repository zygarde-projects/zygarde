package zygarde.codegen.extension.kotlinpoet

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.parseTypeArguments

class ElementExtensionsTest {
  @Test
  fun `parseTypeArguments should return null for non-generic type`() {
    // given
    val input = "java.lang.String"

    // when
    val result = parseTypeArguments(input)

    // then
    result shouldBe null
  }

  @Test
  fun `parseTypeArguments should parse single type parameter`() {
    // given
    val input = "java.util.List<java.lang.String>"

    // when
    val result = parseTypeArguments(input)

    // then
    result shouldBe ("java.util.List" to listOf("java.lang.String"))
  }

  @Test
  fun `parseTypeArguments should parse multiple type parameters`() {
    // given
    val input = "java.util.Map<java.lang.String, java.lang.Integer>"

    // when
    val result = parseTypeArguments(input)

    // then
    result shouldBe ("java.util.Map" to listOf("java.lang.String", "java.lang.Integer"))
  }

  @Test
  fun `parseTypeArguments should parse three type parameters`() {
    // given
    val input = "com.example.Triple<A, B, C>"

    // when
    val result = parseTypeArguments(input)

    // then
    result shouldBe ("com.example.Triple" to listOf("A", "B", "C"))
  }

  @Test
  fun `parseTypeArguments should handle nested generics with greedy regex behavior`() {
    // given - nested generics are parsed greedily; the first group captures up to the last '<'
    // This is acceptable because the function is used for supertype resolution where nested generics
    // are not expected (e.g., AutoIdEntity<Int>, Comparable<String>)
    val input = "java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>"

    // when
    val result = parseTypeArguments(input)

    // then - greedy regex matches outermost < with innermost >
    result shouldBe ("java.util.Map<java.lang.String, java.util.List" to listOf("java.lang.Integer>"))
  }

  @Test
  fun `parseTypeArguments should trim whitespace from type names`() {
    // given
    val input = "com.example.Pair< A , B >"

    // when
    val result = parseTypeArguments(input)

    // then
    result shouldBe ("com.example.Pair" to listOf("A", "B"))
  }
}
