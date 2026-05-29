package zygarde.codegen.dsl.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.dsl.model.type.ValueProviderParameterType

data class ResolvedDtoProviderField(
  val fieldName: String,
  val providerType: ClassName,
  val keyType: TypeName,
  val valueType: TypeName,
  val keySourceFieldName: String,
  val keySourceType: TypeName,
  val keySourceNullable: Boolean,
  val nullable: Boolean,
)

/**
 * A single field of a model-mapping DTO, resolved into the shape downstream code
 * generators (DTO classes, GraphQL SDL, ...) consume. This is the shared
 * structural description so each generator does not re-derive field metadata.
 */
data class ResolvedDtoField(
  val name: String,
  /**
   * Element type without collection wrapping or nullability marker.
   * Meaningful only when [dtoRef] is null (a scalar / enum field).
   */
  val rawType: TypeName,
  val nullable: Boolean,
  val collection: Boolean,
  /** Non-null when this field references another DTO instead of a scalar / enum value. */
  val dtoRef: CodegenDto?,
  /** True when the field is an auto-increment id (mapped via fromAutoIntId / fromAutoLongId). */
  val id: Boolean,
  val comment: String?,
  val modelType: TypeName? = null,
  val modelFieldName: String? = null,
  val modelFieldType: TypeName? = null,
  val modelFieldNullable: Boolean = false,
  val valueProvider: ClassName? = null,
  val valueProviderParameterType: ValueProviderParameterType = ValueProviderParameterType.FIELD,
  val valueProviderParameterField: String? = null,
  val dataProvider: ResolvedDtoProviderField? = null,
)
