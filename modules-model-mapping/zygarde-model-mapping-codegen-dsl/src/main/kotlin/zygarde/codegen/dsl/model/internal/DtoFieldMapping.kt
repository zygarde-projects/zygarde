package zygarde.codegen.dsl.model.internal

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.ValueProvider
import kotlin.reflect.KClass

sealed class DtoFieldMapping(
  open var modelField: ModelMetaField,
  open var dto: CodegenDto,
  open var comment: String? = null,
  open var dtoRef: CodegenDto? = null,
  open var dtoRefClass: ClassName? = null,
  open var refCollection: Boolean = false,
  open var forceNull: ForceNull = ForceNull.NONE,
  open var validations: MutableList<DtoFieldValidation> = mutableListOf(),
  open var compound: Boolean = false,
  open var additionalAnnotations: List<AnnotationSpec> = emptyList(),
) {
  fun nullable() {
    forceNull = ForceNull.NULL
  }

  fun nonNull() {
    forceNull = ForceNull.NOT_NULL
  }

  fun notNull(message: String = "") {
    validations.add(DtoFieldValidation.NotNull(message))
  }

  fun notBlank(message: String = "") {
    validations.add(DtoFieldValidation.NotBlank(message))
  }

  fun notEmpty(message: String = "") {
    validations.add(DtoFieldValidation.NotEmpty(message))
  }

  fun email(message: String = "") {
    validations.add(DtoFieldValidation.Email(message))
  }

  fun regex(regexp: Regex, message: String = "") {
    validations.add(DtoFieldValidation.Regex(regexp, message))
  }

  fun size(min: Int = 0, max: Int = Int.MAX_VALUE, message: String = "") {
    validations.add(DtoFieldValidation.Size(min, max, message))
  }

  fun min(value: Long, message: String = "") {
    validations.add(DtoFieldValidation.Min(value, message))
  }

  fun max(value: Long, message: String = "") {
    validations.add(DtoFieldValidation.Max(value, message))
  }

  fun decimalMin(value: String, inclusive: Boolean = true, message: String = "") {
    validations.add(DtoFieldValidation.DecimalMin(value, inclusive, message))
  }

  fun decimalMax(value: String, inclusive: Boolean = true, message: String = "") {
    validations.add(DtoFieldValidation.DecimalMax(value, inclusive, message))
  }

  fun positive(message: String = "") {
    validations.add(DtoFieldValidation.Positive(message))
  }

  fun positiveOrZero(message: String = "") {
    validations.add(DtoFieldValidation.PositiveOrZero(message))
  }

  fun negative(message: String = "") {
    validations.add(DtoFieldValidation.Negative(message))
  }

  fun negativeOrZero(message: String = "") {
    validations.add(DtoFieldValidation.NegativeOrZero(message))
  }

  fun past(message: String = "") {
    validations.add(DtoFieldValidation.Past(message))
  }

  fun pastOrPresent(message: String = "") {
    validations.add(DtoFieldValidation.PastOrPresent(message))
  }

  fun future(message: String = "") {
    validations.add(DtoFieldValidation.Future(message))
  }

  fun futureOrPresent(message: String = "") {
    validations.add(DtoFieldValidation.FutureOrPresent(message))
  }

  data class DtoFieldNoMapping(
    override var modelField: ModelMetaField,
    override var dto: CodegenDto,
  ) : DtoFieldMapping(
      modelField = modelField,
      dto = dto,
    )

  data class PatchReqFieldMapping(
    override var modelField: ModelMetaField,
    override var dto: CodegenDto,
  ) : DtoFieldMapping(
      modelField = modelField,
      dto = dto,
    )

  data class ModelToDtoFieldMappingVo(
    override var modelField: ModelMetaField,
    override var dto: CodegenDto,
    override var comment: String? = null,
    override var dtoRef: CodegenDto? = null,
    override var dtoRefClass: ClassName? = null,
    override var refCollection: Boolean = false,
    override var forceNull: ForceNull = ForceNull.NONE,
    var valueProvider: ClassName? = null,
    var valueProviderParameterType: ValueProviderParameterType = ValueProviderParameterType.FIELD,
    var valueProviderParameterField: String = modelField.fieldName,
  ) : DtoFieldMapping(
      modelField,
      dto,
      comment,
      dtoRef,
      dtoRefClass,
      refCollection,
      forceNull,
    )

  data class ModelApplyFromDtoFieldMappingVo(
    override var modelField: ModelMetaField,
    override var dto: CodegenDto,
    override var comment: String? = null,
    override var dtoRef: CodegenDto? = null,
    override var dtoRefClass: ClassName? = null,
    override var refCollection: Boolean = false,
    override var forceNull: ForceNull = ForceNull.NONE,
    var valueProvider: KClass<out ValueProvider<*, *>>? = null,
  ) : DtoFieldMapping(
      modelField,
      dto,
      comment,
      dtoRef,
      dtoRefClass,
      refCollection,
      forceNull,
    )
}
