package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.AdditionalDtoProps
import zygarde.codegen.ApiProp
import zygarde.codegen.DtoInherits
import zygarde.codegen.SearchType
import zygarde.codegen.ZygardeModelMappingKaptOptions.ENTITY_PACKAGE_SEARCH
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.isNullable
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.notNullTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.nullableTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.generator.shared.ApiPropSharedGenerator
import zygarde.codegen.generator.shared.DtoFieldDescriptionVo
import zygarde.codegen.generator.shared.validValueProvider
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.persistence.Transient

class ZygardeApiPropGenerator(
  processingEnv: ProcessingEnvironment,
  private val dtoWriteTo: String?,
  private val extensionWriteTo: String?,
) : AbstractZygardeGenerator(processingEnv) {
  val dtoPackageName = packageName("data.dto")

  fun generateModelForZyModelElements(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    elements.forEach { generateModel(it) }
  }

  private fun generateModel(element: Element) {
    val dtoInheritMap = (element.getAnnotation(DtoInherits::class.java)?.value ?: emptyArray())
      .map { it.dto to safeGetTypeFromAnnotation { it.inherit.asTypeName() } }
      .toMap()

    val dtoDescriptionsFromAdditionalDtoProps = (
      element.getAnnotation(AdditionalDtoProps::class.java)
        ?.let { additionalDtoProps ->
          additionalDtoProps.props.flatMap { additionalDtoProp ->
            additionalDtoProp.forDto.map {
              DtoFieldDescriptionVo(
                entityFieldName = additionalDtoProp.field,
                entityFieldType = safeGetTypeFromAnnotation { additionalDtoProp.fieldType.asTypeName() },
                dtoFieldType = safeGetTypeFromAnnotation { additionalDtoProp.fieldType.asTypeName() },
                dtoName = it,
                dtoFieldName = additionalDtoProp.field,
                comment = additionalDtoProp.comment,
                valueProvider = safeGetTypeFromAnnotation { additionalDtoProp.valueProvider.asTypeName() }.validValueProvider(),
                entityValueProvider = safeGetTypeFromAnnotation { additionalDtoProp.entityValueProvider.asTypeName() }.validValueProvider(),
                generateToDtoExtension = true,
                generateApplyToEntityExtension = false
              )
            }
          }
        }
        ?: emptyList()
    )
    val dtoDescriptionsFromElementFields = element
      .allFieldsIncludeSuper()
      .flatMap { fieldElement ->
        val isTransient = fieldElement.getAnnotation(Transient::class.java) != null
        fieldElement.getAnnotationsByType(ApiProp::class.java)
          .flatMap { apiProp ->
            listOf(
              apiProp.dto.flatMap { dto ->
                val refClass = safeGetTypeFromAnnotation { dto.refClass.asTypeName() }.kotlin(dto.refClassNullable)
                val valueProvider = safeGetTypeFromAnnotation { dto.valueProvider.asTypeName() }.kotlin(false).validValueProvider()
                val entityValueProvider = safeGetTypeFromAnnotation { dto.entityValueProvider.asTypeName() }.kotlin(false).validValueProvider()
                dto.names.plus(dto.name).filter { it.isNotEmpty() }.map { dtoName ->
                  toDtoFieldDescription(
                    fieldElement = fieldElement,
                    ref = dto.ref,
                    refNullable = dto.refNullable,
                    refClass = refClass,
                    refCollection = dto.refCollection,
                    dtoName = dtoName,
                    dtoFieldName = dto.fieldName,
                    comment = apiProp.comment,
                    valueProvider = valueProvider,
                    entityValueProvider = entityValueProvider,
                    isTransient = isTransient
                  ).copy(
                    generateToDtoExtension = dto.applyValueFromEntity,
                    generateApplyToEntityExtension = false
                  )
                }
              },
              apiProp.requestDto.flatMap { requestDto ->
                val refClass = safeGetTypeFromAnnotation { requestDto.refClass.asTypeName() }.kotlin(requestDto.refClassNullable)
                val valueProvider = safeGetTypeFromAnnotation { requestDto.valueProvider.asTypeName() }.kotlin(false).validValueProvider()
                requestDto.names.plus(requestDto.name).filter { it.isNotEmpty() }.map { dtoName ->
                  toDtoFieldDescription(
                    fieldElement = fieldElement,
                    ref = requestDto.ref,
                    refNullable = requestDto.refNullable,
                    refClass = refClass,
                    refCollection = requestDto.refCollection,
                    dtoName = dtoName,
                    dtoFieldName = requestDto.fieldName,
                    comment = apiProp.comment,
                    valueProvider = valueProvider,
                    forceNotNull = requestDto.notNullInReq,
                    forceNullable = requestDto.forceNullableInReq,
                    isTransient = isTransient,
                  ).copy(
                    generateToDtoExtension = false,
                    generateApplyToEntityExtension = !isTransient && requestDto.applyValueToEntity && requestDto.searchType == SearchType.NONE,
                    searchType = requestDto.searchType,
                    searchForField = requestDto.searchForField.takeIf { it.isNotEmpty() },
                    sinceApiVersion = requestDto.sinceApiVersion
                  )
                }
              }
            ).flatten()
          }
          .toMutableList()
      }
    val allDescriptions = listOf(dtoDescriptionsFromAdditionalDtoProps, dtoDescriptionsFromElementFields).flatten()
    if (allDescriptions.isEmpty()) return

    val entityTypeName = element.notNullTypeName()
    val searchPackageName = packageName(processingEnv.options.getOrDefault(ENTITY_PACKAGE_SEARCH, "entity.search"))
    val dtoExtensionName = "${element.name()}DtoExtensions"

    val (dtoSpecs, fileBuilderForExtension) = ApiPropSharedGenerator.buildDtoClassesAndExtensions(
      entityTypeName = entityTypeName,
      dtoPackageName = dtoPackageName,
      searchPackageName = searchPackageName,
      dtoExtensionName = dtoExtensionName,
      allDescriptions = allDescriptions,
      dtoInheritMap = dtoInheritMap
    )

    dtoSpecs.forEach { (dtoName, dtoTypeSpec) ->
      FileSpec.builder(dtoPackageName, dtoName)
        .addType(dtoTypeSpec)
        .build()
        .writeTo(
          dtoWriteTo?.let { File(it) } ?: folderToGenerate()
        )
    }

    fileBuilderForExtension.build().writeTo(
      extensionWriteTo?.let { File(it) } ?: folderToGenerate()
    )
  }

  private fun toDtoFieldDescription(
    ref: String,
    refNullable: Boolean,
    refClass: TypeName,
    refCollection: Boolean,
    fieldElement: Element,
    dtoName: String,
    dtoFieldName: String,
    comment: String,
    valueProvider: TypeName? = null,
    entityValueProvider: TypeName? = null,
    forceNotNull: Boolean = false,
    forceNullable: Boolean = false,
    isTransient: Boolean
  ): DtoFieldDescriptionVo {
    val fieldType = when {
      ref.isNotEmpty() -> ClassName(dtoPackageName, ref).let {
        if (refCollection) {
          Collection::class.generic(it.kotlin(refNullable))
        } else {
          it
        }
      }

      refClass.toString() != "java.lang.Object" && refClass.toString() != "kotlin.Any" -> {
        if (refCollection) {
          Collection::class.generic(refClass.kotlin(refClass.isNullable))
        } else {
          refClass
        }
      }

      else -> fieldElement.nullableTypeName()
    }
    val entityFieldName = fieldElement.fieldName()
    if (isTransient && !entityFieldName.startsWith("_")) {
      throw IllegalArgumentException("transient field '$entityFieldName' should be starts with '_'")
    }
    if (!isTransient && entityFieldName.startsWith("_")) {
      throw IllegalArgumentException("field '$entityFieldName' should be annotated with @Transient")
    }
    return DtoFieldDescriptionVo(
      entityFieldName = entityFieldName,
      entityFieldType = fieldElement.typeName(),
      dtoFieldType = fieldType.kotlin(canBeNullable = if (forceNullable) true else !forceNotNull && fieldElement.isNullable()),
      dtoName = dtoName,
      dtoFieldName = (if (dtoFieldName.isNotEmpty()) dtoFieldName else entityFieldName).replaceFirst("_", ""),
      comment = comment,
      dtoRef = ref,
      dtoRefCollection = refCollection,
      valueProvider = valueProvider,
      entityValueProvider = entityValueProvider
    )
  }
}
