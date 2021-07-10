package zygarde.codegen.dsl.model

import zygarde.codegen.dsl.model.type.ValueProviderParameterType

data class EntityFieldToDtoFieldMapping(
  val comment: String,
  val entityField: FieldType?,
  val dtoField: FieldType,
  val valueProviderClassName: String? = null,
  val valueProviderParameterType: ValueProviderParameterType = ValueProviderParameterType.FIELD,
  val dtoRef: String? = null,
  val dtoRefCollection: Boolean = false,
)
