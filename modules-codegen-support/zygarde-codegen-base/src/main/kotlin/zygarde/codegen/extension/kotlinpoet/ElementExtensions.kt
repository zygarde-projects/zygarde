package zygarde.codegen.extension.kotlinpoet

import com.squareup.kotlinpoet.TypeName
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

object ElementExtensions {
  fun Element.name() = simpleName.toString()

  fun Element.fieldName() = simpleName.toString().replaceFirstChar { it.lowercase() }

  fun Element.isNullable() = this.getAnnotation(Nullable::class.java) != null

  fun Element.typeName(): TypeName {
    return typeName(isNullable())
  }

  fun Element.notNullTypeName(): TypeName {
    return typeName(canBeNullable = false)
  }

  fun Element.nullableTypeName(): TypeName {
    return typeName(canBeNullable = true)
  }

  fun Element.isPrimitive(): Boolean {
    return asType().kind.isPrimitive
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

    return listOf(this.enclosedElements, superElements)
      .flatten()
      .filter { it.kind == ElementKind.FIELD }
      .distinctBy { it.fieldName() }
  }

  /**
   * AutoIdEntity<T>
   * AutoIntIdEntity : AutoIdEntity<Int>
   * AutoIdEntity_T -> Int
   */
  fun Element.resolveGenericFieldTypeMap(processingEnv: ProcessingEnvironment): MutableMap<String, TypeName> {
    val superTypes = this.allSuperTypes(processingEnv)
    val allTypeArgs = superTypes.associate { st ->
      val stTypeMirror = st.asType()
      st.toString() to if (stTypeMirror is DeclaredType) {
        stTypeMirror.typeArguments.map { ta -> ta.toString() }
      } else {
        emptyList()
      }
    }

    val genericTypeMap = mutableMapOf<String, TypeName>()
    superTypes.forEach { superType ->
      if (superType is TypeElement) {
        listOf(superType.interfaces, listOf(superType.superclass)).flatten().forEach { superClassOrInterface ->
          val parsed = parseTypeArguments(superClassOrInterface.toString())
          if (parsed != null) {
            val (superClassName, typeClassNames) = parsed
            val typeArgList = allTypeArgs.getOrDefault(superClassName, emptyList())
            typeArgList.forEachIndexed { idx, typeArg ->
              if (idx < typeClassNames.size) {
                val typeClassName = typeClassNames[idx]
                val genericTypePath = "${superClassName}_$typeArg"
                if (typeArg == typeClassName) {
                  val resolvedBySuperType = genericTypeMap[superType.toString() + "_" + typeArg]
                  if (resolvedBySuperType != null) {
                    genericTypeMap[genericTypePath] = resolvedBySuperType
                  }
                } else {
                  val resolvedGenericType = typeClassName.toClassName().kotlin(false)
                  genericTypeMap[genericTypePath] = resolvedGenericType
                }
              }
            }
          }
        }
      }
    }
    return genericTypeMap
  }

  /**
   * Parses a type string like "com.example.Map<K, V>" into a pair of
   * (className, listOf(typeArgNames)).
   * Returns null if the string has no generic type arguments.
   */
  internal fun parseTypeArguments(str: String): Pair<String, List<String>>? {
    val matchResult = "(.*)<(.*)>".toRegex().find(str) ?: return null
    val groupValues = matchResult.groupValues
    if (groupValues.size <= 2) return null
    val className = groupValues[1]
    val typeClassNames = groupValues[2].split(",").map { it.trim() }
    return className to typeClassNames
  }

  private fun Element.typeName(canBeNullable: Boolean = true): TypeName {
    return this.asType().kotlinTypeName(canBeNullable && isNullable())
  }

  // fun Element.tryGetInitializeCodeBlock(): CodeBlock? {
  //   val builder = CodeBlock.Builder()
  //   when (asType().asTypeName().toString()) {
  //     "java.lang.String" -> builder.addStatement("%S", "")
  //     "java.lang.Integer" -> builder.addStatement("%L", "0")
  //     "kotlin.Int" -> builder.addStatement("%L", "0")
  //     "java.lang.Long" -> builder.addStatement("%L", "0L")
  //     "java.lang.Double" -> builder.addStatement("%L", "0.0")
  //     "java.lang.Float" -> builder.addStatement("%L", "0.0")
  //     "java.lang.Short" -> builder.addStatement("%L", "0")
  //     "java.lang.Boolean" -> builder.addStatement("%L", "true")
  //     else -> return null
  //   }
  //   return builder.build()
  // }
}
