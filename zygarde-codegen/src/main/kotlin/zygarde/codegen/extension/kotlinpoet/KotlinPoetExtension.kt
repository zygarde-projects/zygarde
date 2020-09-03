package zygarde.codegen.extension.kotlinpoet

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import org.jetbrains.annotations.Nullable
import kotlin.reflect.KClass

fun Element.name() = simpleName.toString()

fun Element.fieldName() = simpleName.toString().decapitalize()

fun Element.isNullable() = this.getAnnotation(Nullable::class.java) != null

fun Element.typeName(): TypeName {
  return typeName(canBeNullable = false)
}

fun Element.nullableTypeName(): TypeName {
  return typeName(canBeNullable = true)
}

fun Element.isPrimitive(): Boolean {
  return asType().kind.isPrimitive
}

fun Element.tryGetInitializeCodeBlock(): CodeBlock? {
  val builder = CodeBlock.Builder()
  when (asType().asTypeName().toString()) {
    "java.lang.String" -> builder.addStatement("%S", "")
    "java.lang.Integer" -> builder.addStatement("%L", "0")
    "kotlin.Int" -> builder.addStatement("%L", "0")
    "java.lang.Long" -> builder.addStatement("%L", "0L")
    "java.lang.Double" -> builder.addStatement("%L", "0.0")
    "java.lang.Float" -> builder.addStatement("%L", "0.0")
    "java.lang.Short" -> builder.addStatement("%L", "0")
    "java.lang.Boolean" -> builder.addStatement("%L", "true")
    else -> return null
  }
  return builder.build()
}

fun TypeName.kotlin(canBeNullable: Boolean = true): TypeName {
  val typeString = if (this is ParameterizedTypeName) {
    this.rawType.toString()
  } else {
    this.toString()
  }
  val genericTypes = if (this is ParameterizedTypeName) {
    this.typeArguments.map { it.kotlin(it.isNullable) }.toTypedArray()
  } else {
    emptyArray()
  }
  return when (typeString) {
    "java.lang.String" -> String::class.asTypeName()
    "java.lang.Integer" -> Int::class.asTypeName()
    "java.lang.Long" -> Long::class.asTypeName()
    "java.lang.Double" -> Double::class.asTypeName()
    "java.lang.Float" -> Float::class.asTypeName()
    "java.lang.Short" -> Short::class.asTypeName()
    "java.lang.Boolean" -> Boolean::class.asTypeName()
    "java.util.Map" -> Map::class.generic(*genericTypes)
    "java.util.List" -> List::class.generic(*genericTypes)
    "java.util.Set" -> Set::class.generic(*genericTypes)
    "java.util.Collection" -> Collection::class.generic(*genericTypes)
    else -> this
  }.copy(nullable = canBeNullable)
}

fun TypeMirror.kotlinTypeName(canBeNullable: Boolean = true): TypeName {
  return asTypeName().kotlin(canBeNullable)
}

fun Element.allSuperTypes(processingEnv: ProcessingEnvironment): List<Element> {
  val directSupertypes = processingEnv.typeUtils.directSupertypes(this.asType())
  val superElements = directSupertypes
    .flatMap {
      val elements = mutableSetOf<Element>()
      if (it is DeclaredType) {
        val element = it.asElement()
        elements.add(element)
        elements.addAll(element.allSuperTypes(processingEnv))
      }
      elements
    }
  return superElements
}

fun Element.allFieldsIncludeSuper(processingEnv: ProcessingEnvironment): List<Element> {
  val directSupertypes = processingEnv.typeUtils.directSupertypes(this.asType())
  val superElements = directSupertypes
    .flatMap {
      if (it is DeclaredType) {
        it.asElement().allFieldsIncludeSuper(processingEnv)
      } else {
        emptyList()
      }
    }

  return listOf(superElements, this.enclosedElements)
    .flatten()
    .filter { it.kind == ElementKind.FIELD }
}

private fun Element.typeName(canBeNullable: Boolean = true): TypeName {
  return this.asType().kotlinTypeName(canBeNullable && isNullable())
}

fun KClass<*>.generic(vararg typeName: TypeName): TypeName {
  return asClassName().parameterizedBy(
    *typeName
  )
}
