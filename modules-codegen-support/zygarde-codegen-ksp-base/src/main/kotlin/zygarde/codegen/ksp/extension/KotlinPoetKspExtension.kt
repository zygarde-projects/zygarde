package zygarde.codegen.ksp.extension

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
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

fun KSAnnotation.getArgumentValue(name: String): Any? {
  return arguments.find { it.name?.asString() == name }?.value
}

fun KSAnnotation.getArgumentValueAsString(name: String): String {
  return getArgumentValue(name) as? String ?: ""
}

fun KSAnnotation.getArgumentValueAsBoolean(name: String, default: Boolean = false): Boolean {
  return getArgumentValue(name) as? Boolean ?: default
}

fun KSAnnotation.getArgumentValueAsLong(name: String, default: Long = 0): Long {
  return (getArgumentValue(name) as? Number)?.toLong() ?: default
}

@Suppress("UNCHECKED_CAST")
fun KSAnnotation.getArgumentValueAsStringArray(name: String): List<String> {
  return (getArgumentValue(name) as? List<String>) ?: emptyList()
}

@Suppress("UNCHECKED_CAST")
fun KSAnnotation.getArgumentValueAsAnnotationList(name: String): List<KSAnnotation> {
  return (getArgumentValue(name) as? List<KSAnnotation>) ?: emptyList()
}

fun KSAnnotation.getArgumentValueAsType(name: String): KSType? {
  return getArgumentValue(name) as? KSType
}

fun KSAnnotation.getArgumentValueAsTypeName(name: String): TypeName? {
  return getArgumentValueAsType(name)?.toTypeName()?.kotlin(false)
}

fun KSAnnotation.getArgumentValueAsEnumEntry(name: String): String? {
  val value = getArgumentValue(name) ?: return null
  return if (value is KSType) {
    value.declaration.simpleName.asString()
  } else {
    value.toString()
  }
}

fun KSPropertyDeclaration.isNullable(): Boolean {
  return type.resolve().isMarkedNullable
}

fun KSPropertyDeclaration.fieldName(): String {
  return simpleName.asString()
}

fun KSClassDeclaration.name(): String {
  return simpleName.asString()
}

fun KSClassDeclaration.fieldName(): String {
  return simpleName.asString().replaceFirstChar { it.lowercase() }
}

fun KSClassDeclaration.getAllPropertiesIncludingSuper(): List<KSPropertyDeclaration> {
  val props = mutableListOf<KSPropertyDeclaration>()
  props.addAll(getAllProperties().toList())

  superTypes.forEach { superTypeRef ->
    val superType = superTypeRef.resolve()
    val superDeclaration = superType.declaration
    if (superDeclaration is KSClassDeclaration) {
      val superTypeName = superDeclaration.qualifiedName?.asString()
      if (superTypeName != null &&
        !superTypeName.startsWith("kotlin.") &&
        !superTypeName.startsWith("java.")
      ) {
        props.addAll(superDeclaration.getAllPropertiesIncludingSuper())
      }
    }
  }

  return props.distinctBy { it.simpleName.asString() }
}

fun KSClassDeclaration.implementsInterface(interfaceName: String): Boolean {
  return superTypes.any { superTypeRef ->
    val superType = superTypeRef.resolve()
    superType.declaration.qualifiedName?.asString() == interfaceName
  }
}
