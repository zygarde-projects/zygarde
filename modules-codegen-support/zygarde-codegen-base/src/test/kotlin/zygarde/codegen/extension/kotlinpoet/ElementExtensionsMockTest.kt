package zygarde.codegen.extension.kotlinpoet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.annotations.Nullable
import org.junit.jupiter.api.Test
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allFieldsIncludeSuper
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allSuperTypes
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.isNullable
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.notNullTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.nullableTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.resolveGenericFieldTypeMap
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.NoType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

class ElementExtensionsMockTest {
  private fun name(value: String) = object : Name {
    override val length: Int = value.length

    override fun get(index: Int): Char = value[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

    override fun contentEquals(cs: CharSequence): Boolean = value.contentEquals(cs)

    override fun toString(): String = value
  }

  private fun primitive(type: String) = mockk<PrimitiveType>().also {
    every { it.toString() } returns type
  }

  private fun plainType(type: String) = mockk<TypeMirror>().also {
    every { it.toString() } returns type
  }

  private fun declared(type: String, args: List<TypeMirror> = emptyList(), element: Element = mockk()) = mockk<DeclaredType>().also {
    every { it.toString() } returns type
    every { it.typeArguments } returns args
    every { it.asElement() } returns element
  }

  private fun element(simpleName: String, type: TypeMirror = primitive("int"), kind: ElementKind = ElementKind.FIELD) = mockk<Element>().also {
    every { it.simpleName } returns name(simpleName)
    every { it.asType() } returns type
    every { it.kind } returns kind
    every { it.getAnnotation(Nullable::class.java) } returns null
    every { it.enclosedElements } returns emptyList()
    every { it.toString() } returns simpleName
  }

  @Test
  fun `Element helpers should expose names nullability and type names`() {
    val nullableAnnotation = mockk<Nullable>()
    val nullableElement = element("UserName", primitive("int")).also {
      every { it.getAnnotation(Nullable::class.java) } returns nullableAnnotation
    }

    nullableElement.name() shouldBe "UserName"
    nullableElement.fieldName() shouldBe "userName"
    nullableElement.isNullable() shouldBe true
    nullableElement.typeName().toString() shouldBe "kotlin.Int?"
    nullableElement.notNullTypeName().toString() shouldBe "kotlin.Int"
    nullableElement.nullableTypeName().toString() shouldBe "kotlin.Int?"
  }

  @Test
  fun `TypeMirror kotlinTypeName should handle primitives declared generics and unknown primitive`() {
    primitive("byte").kotlinTypeName(false).toString() shouldBe "kotlin.Short"
    primitive("short").kotlinTypeName(false).toString() shouldBe "kotlin.Short"
    primitive("int").kotlinTypeName(false).toString() shouldBe "kotlin.Int"
    primitive("long").kotlinTypeName(false).toString() shouldBe "kotlin.Long"
    primitive("float").kotlinTypeName(false).toString() shouldBe "kotlin.Float"
    primitive("double").kotlinTypeName(false).toString() shouldBe "kotlin.Double"
    primitive("boolean").kotlinTypeName(false).toString() shouldBe "kotlin.Boolean"
    primitive("string").kotlinTypeName(false).toString() shouldBe "kotlin.String"
    shouldThrow<IllegalArgumentException> {
      primitive("char").kotlinTypeName(false)
    }.message shouldBe "unable to resolve char"

    declared("java.lang.String").kotlinTypeName(false).toString() shouldBe "kotlin.String"
    declared("java.util.List<java.lang.String>", listOf(plainType("java.lang.String")), element = element("java.util.List"))
      .kotlinTypeName(false)
      .toString() shouldBe "java.util.List<kotlin.String>"
    plainType("? extends java.lang.String").kotlinTypeName(false).toString() shouldBe "kotlin.String"
  }

  @Test
  fun `Element hierarchy helpers should walk supertypes and include inherited fields once`() {
    val processingEnv = mockk<ProcessingEnvironment>()
    val types = mockk<Types>()
    every { processingEnv.typeUtils } returns types

    val rootType = plainType("com.example.Child")
    val superTypeElement = element("com.example.Parent", plainType("com.example.Parent"), ElementKind.CLASS)
    val superType = declared("com.example.Parent", element = superTypeElement)
    val root = element("Child", rootType, ElementKind.CLASS)
    val rootField = element("id")
    val inheritedField = element("parentName", primitive("long"))
    val duplicateInheritedField = element("id", primitive("long"))

    every { types.directSupertypes(rootType) } returns listOf(superType)
    every { types.directSupertypes(superTypeElement.asType()) } returns emptyList()
    every { root.enclosedElements } returns listOf(rootField)
    every { superTypeElement.enclosedElements } returns listOf(inheritedField, duplicateInheritedField)

    root.allSuperTypes(processingEnv) shouldContainExactly listOf(superTypeElement)
    root.allFieldsIncludeSuper(processingEnv) shouldContainExactly listOf(rootField, inheritedField)
  }

  @Test
  fun `resolveGenericFieldTypeMap should resolve inherited generic arguments`() {
    val processingEnv = mockk<ProcessingEnvironment>()
    val types = mockk<Types>()
    every { processingEnv.typeUtils } returns types

    val rootType = plainType("com.example.Child")
    val root = element("Child", rootType, ElementKind.CLASS)
    val child = mockk<TypeElement>()
    val base = mockk<TypeElement>()
    val childType = declared("com.example.Child", listOf(plainType("ID")), child)
    val baseType = declared("com.example.Base", listOf(plainType("ID")), base)
    val childExtendsBase = declared("com.example.Base<java.lang.Integer>", element = base)
    val noType = mockk<NoType>().also { every { it.toString() } returns "none" }

    every { child.asType() } returns childType
    every { child.toString() } returns "com.example.Child"
    every { child.interfaces } returns emptyList()
    every { child.superclass } returns childExtendsBase
    every { base.asType() } returns baseType
    every { base.toString() } returns "com.example.Base"
    every { base.interfaces } returns emptyList()
    every { base.superclass } returns noType

    every { types.directSupertypes(rootType) } returns listOf(declared("com.example.Child", element = child))
    every { types.directSupertypes(childType) } returns listOf(declared("com.example.Base", element = base))
    every { types.directSupertypes(baseType) } returns emptyList()

    root.resolveGenericFieldTypeMap(processingEnv) shouldContain ("com.example.Base_ID" to Int::class.asTypeName())
  }
}
