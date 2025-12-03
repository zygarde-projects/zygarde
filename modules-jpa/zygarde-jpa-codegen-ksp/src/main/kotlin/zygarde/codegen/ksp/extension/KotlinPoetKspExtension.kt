package zygarde.codegen.ksp.extension

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
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
  val typeString = this.toString().removeSuffix("?")
  return when (typeString) {
    "java.lang.Object" -> Any::class.asTypeName()
    "java.lang.String" -> String::class.asTypeName()
    "java.lang.Integer" -> Int::class.asTypeName()
    "java.lang.Long" -> Long::class.asTypeName()
    "java.lang.Double" -> Double::class.asTypeName()
    "java.lang.Float" -> Float::class.asTypeName()
    "java.lang.Short" -> Short::class.asTypeName()
    "java.lang.Boolean" -> Boolean::class.asTypeName()
    else -> this
  }.copy(nullable = canBeNullable)
}

fun ClassName.generic(vararg typeName: TypeName): TypeName {
  return if (typeName.isNotEmpty()) {
    this.parameterizedBy(*typeName)
  } else {
    this
  }
}

fun KClass<*>.generic(vararg typeName: TypeName): TypeName {
  val className = asClassName()
  return if (typeName.isNotEmpty()) {
    className.parameterizedBy(*typeName)
  } else {
    className
  }
}
