package zygarde.codegen.dsl.extensions

import zygarde.codegen.meta.Comment
import zygarde.codegen.meta.ModelMetaField
import java.lang.reflect.ParameterizedType
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

fun KProperty1<*, *>.asModelMetaField(): ModelMetaField<*, *> {
  val comment = this.javaField?.getAnnotation(Comment::class.java)?.comment.orEmpty()
  if (this is CallableReference) {
    val owner = this.owner
    if (owner is KClass<*>) {
      val returnTypeClassifier = this.returnType.classifier
      if (returnTypeClassifier is KClass<*>) {
        val returnTypeJavaType = returnType.javaType
        val genericClasses = if (returnTypeJavaType is ParameterizedType) {
          returnTypeJavaType.actualTypeArguments
            .map { ta ->
              (ta as Class<*>).kotlin
            }
            .toTypedArray()
        } else {
          emptyArray()
        }
        return ModelMetaField(
          modelClass = owner,
          fieldName = this.name,
          fieldClass = returnTypeClassifier,
          fieldNullable = this.returnType.isMarkedNullable,
          extra = false,
          genericClasses = genericClasses,
          comment = comment
        )
      }
    }
  }
  throw IllegalArgumentException("unable to resolve $this as ModelMetaField")
}
