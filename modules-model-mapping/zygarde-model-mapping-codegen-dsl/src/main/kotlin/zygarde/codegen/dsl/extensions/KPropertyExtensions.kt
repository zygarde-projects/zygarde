package zygarde.codegen.dsl.extensions

import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.meta.ModelMetaField
import zygarde.core.annotation.Comment
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import org.hibernate.annotations.Comment as HibernateComment

fun KProperty1<*, *>.asModelMetaField(): ModelMetaField {
  val comment = this.javaField?.getAnnotation(HibernateComment::class.java)?.value
    ?: this.javaField?.getAnnotation(Comment::class.java)?.comment
  if (this is CallableReference) {
    val owner = this.owner
    if (owner is KClass<*>) {
      return ModelMetaField(
        modelClass = owner.asTypeName(),
        fieldName = this.name,
        fieldClass = this.returnType.asTypeName(),
        fieldNullable = this.returnType.isMarkedNullable,
        extra = false,
        comment = comment.orEmpty(),
      )
    }
  }
  throw IllegalArgumentException("unable to resolve $this as ModelMetaField")
}
