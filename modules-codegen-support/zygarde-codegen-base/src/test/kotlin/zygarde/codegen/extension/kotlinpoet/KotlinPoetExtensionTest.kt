package zygarde.codegen.extension.kotlinpoet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KotlinPoetExtensionTest {
  @Test
  fun `toClassName should split package and simple name`() {
    "com.example.Foo".toClassName() shouldBe ClassName("com.example", "Foo")
  }

  @Test
  fun `kotlin should convert Java scalar type names and nullability`() {
    ClassName("java.lang", "Object").kotlin(false) shouldBe Any::class.asTypeName()
    ClassName("java.lang", "String").kotlin(false) shouldBe String::class.asTypeName()
    ClassName("java.lang", "Integer").kotlin(false) shouldBe Int::class.asTypeName()
    ClassName("java.lang", "Long").kotlin(false) shouldBe Long::class.asTypeName()
    ClassName("java.lang", "Double").kotlin(false) shouldBe Double::class.asTypeName()
    ClassName("java.lang", "Float").kotlin(false) shouldBe Float::class.asTypeName()
    ClassName("java.lang", "Short").kotlin(false) shouldBe Short::class.asTypeName()
    ClassName("java.lang", "Boolean").kotlin(false) shouldBe Boolean::class.asTypeName()
    ClassName("com.example", "Custom").kotlin(true) shouldBe ClassName("com.example", "Custom").copy(nullable = true)
  }

  @Test
  fun `kotlin should convert Java collection type names recursively`() {
    val javaString = ClassName("java.lang", "String")
    val javaInteger = ClassName("java.lang", "Integer")

    ClassName("java.util", "List")
      .parameterizedBy(javaString)
      .kotlin(false)
      .toString() shouldBe "kotlin.collections.List<kotlin.String>"
    ClassName("java.util", "Set")
      .parameterizedBy(javaInteger)
      .kotlin(false)
      .toString() shouldBe "kotlin.collections.Set<kotlin.Int>"
    ClassName("java.util", "Collection")
      .parameterizedBy(javaString.copy(nullable = true))
      .kotlin(true)
      .toString() shouldBe "kotlin.collections.Collection<kotlin.String?>?"
    ClassName("java.util", "Map")
      .parameterizedBy(javaString, javaInteger)
      .kotlin(false)
      .toString() shouldBe "kotlin.collections.Map<kotlin.String, kotlin.Int>"
  }

  @Test
  fun `generic should parameterize ClassName and reject other TypeName implementations`() {
    String::class.asClassName().generic() shouldBe String::class.asClassName()
    List::class.asClassName().generic(String::class.asTypeName()).toString() shouldBe "kotlin.collections.List<kotlin.String>"

    shouldThrow<UnsupportedOperationException> {
      TypeVariableName("T").generic(String::class.asTypeName())
    }.message shouldBe "generic only supported by ClassName"
  }

  @Test
  fun `KClass generic should support Java Type and TypeName arguments`() {
    List::class.generic(String::class.java).toString() shouldBe "kotlin.collections.List<kotlin.String>"
    Map::class.generic(String::class.asTypeName(), Int::class.asTypeName()).toString() shouldBe
      "kotlin.collections.Map<kotlin.String, kotlin.Int>"
  }
}
