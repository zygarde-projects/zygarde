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

  data class DtoFieldNoMapping(
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
