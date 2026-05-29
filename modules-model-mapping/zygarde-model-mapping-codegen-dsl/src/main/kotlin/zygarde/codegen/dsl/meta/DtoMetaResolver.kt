package zygarde.codegen.dsl.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.AutoLongIdValueProvider

/**
 * The single source of truth that turns raw [DtoFieldMapping] declarations into
 * resolved field metadata. Both the DTO class generator and the GraphQL SDL
 * deriver go through here so the generated DTO and its GraphQL type never drift.
 */
object DtoMetaResolver {
  const val DEFAULT_DTO_PACKAGE = "zygarde.codegen.data.dto"

  private val collectionRawTypes = setOf(
    Collection::class.asClassName(),
    List::class.asClassName(),
    Set::class.asClassName(),
    Iterable::class.asClassName(),
  )

  fun dtoPackageName(): String =
    System.getProperty("zygarde.codegen.dsl.model-mapping.dto-package", DEFAULT_DTO_PACKAGE)

  /** Whether the generated DTO field is nullable, applying the optional [ForceNull] override. */
  fun resolveFieldNullable(mapping: DtoFieldMapping): Boolean =
    when (mapping.forceNull) {
      ForceNull.NONE -> mapping.modelField.fieldNullable
      ForceNull.NULL -> true
      ForceNull.NOT_NULL -> false
    }

  /** The fully resolved Kotlin type of a generated DTO field (collection-wrapped, nullability marked). */
  fun resolveFieldType(mapping: DtoFieldMapping, dtoPackageName: String = dtoPackageName()): TypeName {
    val fieldTypeNullable = resolveFieldNullable(mapping)
    return (
      mapping.dtoRefClass
        ?: mapping.dtoRef?.let { ClassName(dtoPackageName, it.name) }
        ?: (mapping as? DtoFieldMapping.ModelToDtoFieldMappingVo)?.dataProviderValueType
        ?: mapping.modelField.fieldClass
    )
      .kotlin(!mapping.refCollection && fieldTypeNullable)
      .let { if (mapping.refCollection) Collection::class.generic(it).kotlin(fieldTypeNullable) else it }
  }

  /**
   * Collect every DTO declared across [mappings] into resolved field metadata.
   * Field ordering and last-write-wins de-duplication mirror the DTO class generator.
   */
  fun resolve(
    mappings: Collection<DtoFieldMapping>,
    dtoPackageName: String = dtoPackageName(),
  ): ModelMappingMetadata {
    val fieldsByDto = mappings
      .groupBy { it.dto }
      .mapValues { (_, dtoMappings) ->
        dtoMappings
          .associateBy { it.modelField.fieldName }
          .values
          .map { it.toResolvedDtoField(dtoPackageName) }
      }
    return ModelMappingMetadata(fieldsByDto)
  }

  private fun DtoFieldMapping.toResolvedDtoField(dtoPackageName: String): ResolvedDtoField {
    val declaredType = (
      dtoRefClass
        ?: dtoRef?.let { ClassName(dtoPackageName, it.name) }
        ?: (this as? DtoFieldMapping.ModelToDtoFieldMappingVo)?.dataProviderValueType
        ?: modelField.fieldClass
    ).kotlin(false)
    return ResolvedDtoField(
      name = modelField.fieldName,
      rawType = if (refCollection) declaredType.collectionElementOrSelf() else declaredType,
      nullable = resolveFieldNullable(this),
      collection = refCollection,
      dtoRef = dtoRef,
      id = isAutoId(),
      comment = (comment ?: modelField.comment).takeUnless { it.isNullOrBlank() },
    )
  }

  private fun TypeName.collectionElementOrSelf(): TypeName {
    if (this is ParameterizedTypeName && rawType in collectionRawTypes) {
      return typeArguments.firstOrNull()?.copy(nullable = false) ?: this
    }
    return this
  }

  private fun DtoFieldMapping.isAutoId(): Boolean {
    if (this !is DtoFieldMapping.ModelToDtoFieldMappingVo) return false
    return valueProvider == AutoIntIdValueProvider::class.asClassName() ||
      valueProvider == AutoLongIdValueProvider::class.asClassName()
  }
}
