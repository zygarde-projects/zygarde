package zygarde.codegen.dsl.model

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.extension.kotlinpoet.toClassName

data class FieldType(
  val fieldName: String,
  val typeClassName: String,
  val nullable: Boolean = false,
  val typeClassGenericParameters: List<String> = emptyList(),
) {
  fun kotlinType(): TypeName {
    return typeClassName.toClassName()
      .let { type ->
        if (typeClassGenericParameters.isNotEmpty()) {
          type.parameterizedBy(typeClassGenericParameters.map { p -> p.toClassName().kotlin(false) })
        } else {
          type
        }
      }
      .kotlin(nullable)
  }
}
