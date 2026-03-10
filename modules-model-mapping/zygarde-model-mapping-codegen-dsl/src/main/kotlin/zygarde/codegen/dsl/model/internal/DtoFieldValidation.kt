package zygarde.codegen.dsl.model.internal

import com.squareup.kotlinpoet.AnnotationSpec
import javax.validation.constraints.Pattern

sealed class DtoFieldValidation(
  open val message: String,
) {
  abstract fun buildAnnotation(): AnnotationSpec

  protected fun AnnotationSpec.Builder.fieldTarget(): AnnotationSpec.Builder =
    useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)

  protected fun AnnotationSpec.Builder.withMessage(message: String): AnnotationSpec.Builder =
    if (message.isNotEmpty()) addMember("message=%S", message) else this

  data class Regex(
    val regexp: kotlin.text.Regex,
    override val message: String,
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(Pattern::class)
      .fieldTarget()
      .addMember("regexp=%S", regexp.pattern)
      .withMessage(message)
      .build()
  }

  data class Email(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Email::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class NotNull(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.NotNull::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class NotBlank(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.NotBlank::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class NotEmpty(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.NotEmpty::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class Size(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Size::class)
      .fieldTarget()
      .addMember("min=%L", min)
      .addMember("max=%L", max)
      .withMessage(message)
      .build()
  }

  data class Min(
    val value: Long,
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Min::class)
      .fieldTarget()
      .addMember("value=%LL", value)
      .withMessage(message)
      .build()
  }

  data class Max(
    val value: Long,
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Max::class)
      .fieldTarget()
      .addMember("value=%LL", value)
      .withMessage(message)
      .build()
  }

  data class DecimalMin(
    val value: String,
    val inclusive: Boolean = true,
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.DecimalMin::class)
      .fieldTarget()
      .addMember("value=%S", value)
      .addMember("inclusive=%L", inclusive)
      .withMessage(message)
      .build()
  }

  data class DecimalMax(
    val value: String,
    val inclusive: Boolean = true,
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.DecimalMax::class)
      .fieldTarget()
      .addMember("value=%S", value)
      .addMember("inclusive=%L", inclusive)
      .withMessage(message)
      .build()
  }

  data class Positive(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Positive::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class PositiveOrZero(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.PositiveOrZero::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class Negative(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Negative::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class NegativeOrZero(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.NegativeOrZero::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class Past(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Past::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class PastOrPresent(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.PastOrPresent::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class Future(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.Future::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }

  data class FutureOrPresent(
    override val message: String = "",
  ) : DtoFieldValidation(message) {
    override fun buildAnnotation(): AnnotationSpec = AnnotationSpec.builder(javax.validation.constraints.FutureOrPresent::class)
      .fieldTarget()
      .withMessage(message)
      .build()
  }
}
