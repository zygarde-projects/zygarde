package zygarde.codegen.extension.kotlinpoet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

fun String.toClassName(): ClassName {
  val lastDot = this.lastIndexOf(".")
  val typeName = this.substring(lastDot + 1)
  return ClassName(
    this.substring(0, lastDot),
    typeName,
  )
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

fun KClass<*>.generic(vararg genericClasses: KClass<*>): TypeName {
  val className = asClassName()
  if (genericClasses.isNotEmpty()) {
    return className.parameterizedBy(
      *genericClasses.map { it.asTypeName() }.toTypedArray()
    )
  }
  return className
}

fun KClass<*>.generic(vararg typeName: TypeName): TypeName {
  val className = asClassName()
  if (typeName.isNotEmpty()) {
    return className.parameterizedBy(
      *typeName
    )
  }
  return className
}
