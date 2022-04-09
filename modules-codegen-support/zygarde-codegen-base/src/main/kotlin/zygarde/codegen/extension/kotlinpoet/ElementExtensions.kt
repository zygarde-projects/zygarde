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
      } else emptyList()
    }

    val genericTypeMap = mutableMapOf<String, TypeName>()
    superTypes.forEach { superType ->
      if (superType is TypeElement) {
        listOf(superType.interfaces, listOf(superType.superclass)).flatten().forEach { superClassOrInterface ->
          val matchedValues = "(.*)<(.*)>".toRegex().find(superClassOrInterface.toString())?.groupValues ?: emptyList()
          if (matchedValues.size > 1) {
            val superClassName = matchedValues[1]
            val typeArgList = allTypeArgs.getOrDefault(superClassName, emptyList())
            typeArgList.forEachIndexed { idx, typeArg ->
              val typeClassName = matchedValues[idx + 2]
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
    return genericTypeMap
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
