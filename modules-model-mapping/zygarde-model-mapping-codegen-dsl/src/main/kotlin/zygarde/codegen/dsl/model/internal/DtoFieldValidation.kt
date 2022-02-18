package zygarde.codegen.dsl.model.internal

import com.squareup.kotlinpoet.AnnotationSpec
import javax.validation.constraints.Pattern

sealed class DtoFieldValidation(
  open val message: String,
) {
  abstract fun buildAnnotation(): AnnotationSpec

  data class Regex(
    val regexp: kotlin.text.Regex,
    override val message: String,
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(Pattern::class)
      .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
      .addMember("regexp=%S", regexp.pattern)
      .addMember("message=%S", message)
      .build()
  }

  data class Email(
    override val message: String,
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Email::class).build()
  }
}
