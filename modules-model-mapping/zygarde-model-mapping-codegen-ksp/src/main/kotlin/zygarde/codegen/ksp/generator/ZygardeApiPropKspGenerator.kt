package zygarde.codegen.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import zygarde.codegen.SearchType
import zygarde.codegen.ksp.ZygardeModelMappingKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.ZygardeModelMappingKspOptions.ENTITY_PACKAGE_SEARCH
import zygarde.codegen.ksp.ZygardeModelMappingKspOptions.MODEL_MAPPING_DTO_PACKAGE
import zygarde.codegen.ksp.extension.fieldName
import zygarde.codegen.ksp.extension.generic
import zygarde.codegen.ksp.extension.getArgumentValueAsAnnotationList
import zygarde.codegen.ksp.extension.getArgumentValueAsBoolean
import zygarde.codegen.ksp.extension.getArgumentValueAsEnumEntry
import zygarde.codegen.ksp.extension.getArgumentValueAsLong
import zygarde.codegen.ksp.extension.getArgumentValueAsString
import zygarde.codegen.ksp.extension.getArgumentValueAsStringArray
import zygarde.codegen.ksp.extension.getArgumentValueAsType
import zygarde.codegen.ksp.extension.getArgumentValueAsTypeName
import zygarde.codegen.ksp.extension.getAllPropertiesIncludingSuper
import zygarde.codegen.ksp.extension.isNullable
import zygarde.codegen.ksp.extension.kotlin
import zygarde.codegen.ksp.extension.name
import zygarde.codegen.generator.shared.ApiPropSharedGenerator
import zygarde.codegen.generator.shared.DtoFieldDescriptionVo
import zygarde.codegen.generator.shared.validValueProvider

class ZygardeApiPropKspGenerator(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String>
) {
  private fun packageName(pack: String): String {
    val basePackage = options.getOrDefault(BASE_PACKAGE, "zygarde.generated")
    return "$basePackage.$pack"
  }

  private val dtoPackageName by lazy {
    packageName(options.getOrDefault(MODEL_MAPPING_DTO_PACKAGE, "data.dto"))
  }

  fun generateModelForZyModelElements(elements: Collection<KSClassDeclaration>) {
    if (elements.isEmpty()) {
      return
    }
    elements.forEach { generateModel(it) }
  }

  private fun generateModel(element: KSClassDeclaration) {
    val dtoInheritMap = element.annotations
      .find { it.shortName.asString() == "DtoInherits" }
      ?.getArgumentValueAsAnnotationList("value")
      ?.mapNotNull { dtoInheritAnn ->
        val dto = dtoInheritAnn.getArgumentValueAsString("dto")
        val inheritType = dtoInheritAnn.getArgumentValueAsType("inherit")?.toTypeName()
        if (dto.isNotEmpty() && inheritType != null) dto to inheritType else null
      }
      ?.toMap()
      ?: emptyMap()

    val dtoDescriptionsFromAdditionalDtoProps = element.annotations
      .find { it.shortName.asString() == "AdditionalDtoProps" }
      ?.getArgumentValueAsAnnotationList("props")
      ?.flatMap { additionalDtoProp ->
        val forDto = additionalDtoProp.getArgumentValueAsStringArray("forDto")
        val field = additionalDtoProp.getArgumentValueAsString("field")
        val fieldType = additionalDtoProp.getArgumentValueAsTypeName("fieldType") ?: return@flatMap emptyList()
        val comment = additionalDtoProp.getArgumentValueAsString("comment")
        val valueProvider = additionalDtoProp.getArgumentValueAsTypeName("valueProvider").validValueProvider()
        val entityValueProvider = additionalDtoProp.getArgumentValueAsTypeName("entityValueProvider").validValueProvider()

        forDto.map { dtoName ->
          DtoFieldDescriptionVo(
            entityFieldName = field,
            entityFieldType = fieldType,
            dtoFieldType = fieldType,
            dtoName = dtoName,
            dtoFieldName = field,
            comment = comment,
            valueProvider = valueProvider,
            entityValueProvider = entityValueProvider,
            generateToDtoExtension = true,
            generateApplyToEntityExtension = false
          )
        }
      }
      ?: emptyList()

    val dtoDescriptionsFromElementFields = element
      .getAllPropertiesIncludingSuper()
      .flatMap { fieldElement ->
        val isTransient = fieldElement.annotations.any { it.shortName.asString() == "Transient" }
        fieldElement.annotations
          .filter { it.shortName.asString() == "ApiProp" }
          .flatMap { apiProp ->
            val comment = apiProp.getArgumentValueAsString("comment")
            val dtoList = apiProp.getArgumentValueAsAnnotationList("dto")
            val requestDtoList = apiProp.getArgumentValueAsAnnotationList("requestDto")

            val dtoDescriptions = dtoList.flatMap { dto ->
              processDtoAnnotation(fieldElement, dto, comment, isTransient)
            }

            val requestDtoDescriptions = requestDtoList.flatMap { requestDto ->
              processRequestDtoAnnotation(fieldElement, requestDto, comment, isTransient)
            }

            dtoDescriptions + requestDtoDescriptions
          }
      }

    val allDescriptions = dtoDescriptionsFromAdditionalDtoProps + dtoDescriptionsFromElementFields
    if (allDescriptions.isEmpty()) return

    val entityTypeName = element.asType(emptyList()).toTypeName().copy(nullable = false)
    val searchPackageName = packageName(options.getOrDefault(ENTITY_PACKAGE_SEARCH, "entity.search"))
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
        .writeTo(codeGenerator, aggregating = false)
    }

    fileBuilderForExtension.build().writeTo(codeGenerator, aggregating = false)
  }

  private fun processDtoAnnotation(
    fieldElement: KSPropertyDeclaration,
    dto: KSAnnotation,
    comment: String,
    isTransient: Boolean
  ): List<DtoFieldDescriptionVo> {
    val name = dto.getArgumentValueAsString("name")
    val names = dto.getArgumentValueAsStringArray("names")
    val fieldName = dto.getArgumentValueAsString("fieldName")
    val ref = dto.getArgumentValueAsString("ref")
    val refNullable = dto.getArgumentValueAsBoolean("refNullable")
    val refCollection = dto.getArgumentValueAsBoolean("refCollection")
    val refClass = dto.getArgumentValueAsTypeName("refClass")?.kotlin(dto.getArgumentValueAsBoolean("refClassNullable"))
    val applyValueFromEntity = dto.getArgumentValueAsBoolean("applyValueFromEntity", true)
    val valueProvider = dto.getArgumentValueAsTypeName("valueProvider").validValueProvider()
    val entityValueProvider = dto.getArgumentValueAsTypeName("entityValueProvider").validValueProvider()

    val allNames = (listOf(name) + names).filter { it.isNotEmpty() }

    return allNames.map { dtoName ->
      toDtoFieldDescription(
        fieldElement = fieldElement,
        ref = ref,
        refNullable = refNullable,
        refClass = refClass,
        refCollection = refCollection,
        dtoName = dtoName,
        dtoFieldName = fieldName,
        comment = comment,
        valueProvider = valueProvider,
        entityValueProvider = entityValueProvider,
        isTransient = isTransient
      ).copy(
        generateToDtoExtension = applyValueFromEntity,
        generateApplyToEntityExtension = false
      )
    }
  }

  private fun processRequestDtoAnnotation(
    fieldElement: KSPropertyDeclaration,
    requestDto: KSAnnotation,
    comment: String,
    isTransient: Boolean
  ): List<DtoFieldDescriptionVo> {
    val name = requestDto.getArgumentValueAsString("name")
    val names = requestDto.getArgumentValueAsStringArray("names")
    val fieldName = requestDto.getArgumentValueAsString("fieldName")
    val ref = requestDto.getArgumentValueAsString("ref")
    val refNullable = requestDto.getArgumentValueAsBoolean("refNullable")
    val refCollection = requestDto.getArgumentValueAsBoolean("refCollection")
    val refClass = requestDto.getArgumentValueAsTypeName("refClass")?.kotlin(requestDto.getArgumentValueAsBoolean("refClassNullable"))
    val applyValueToEntity = requestDto.getArgumentValueAsBoolean("applyValueToEntity", true)
    val valueProvider = requestDto.getArgumentValueAsTypeName("valueProvider").validValueProvider()
    val searchTypeStr = requestDto.getArgumentValueAsEnumEntry("searchType") ?: "NONE"
    val searchType = SearchType.valueOf(searchTypeStr)
    val searchForField = requestDto.getArgumentValueAsString("searchForField").takeIf { it.isNotEmpty() }
    val notNullInReq = requestDto.getArgumentValueAsBoolean("notNullInReq")
    val forceNullableInReq = requestDto.getArgumentValueAsBoolean("forceNullableInReq")
    val sinceApiVersion = requestDto.getArgumentValueAsLong("sinceApiVersion")

    val allNames = (listOf(name) + names).filter { it.isNotEmpty() }

    return allNames.map { dtoName ->
      toDtoFieldDescription(
        fieldElement = fieldElement,
        ref = ref,
        refNullable = refNullable,
        refClass = refClass,
        refCollection = refCollection,
        dtoName = dtoName,
        dtoFieldName = fieldName,
        comment = comment,
        valueProvider = valueProvider,
        forceNotNull = notNullInReq,
        forceNullable = forceNullableInReq,
        isTransient = isTransient
      ).copy(
        generateToDtoExtension = false,
        generateApplyToEntityExtension = !isTransient && applyValueToEntity && searchType == SearchType.NONE,
        searchType = searchType,
        searchForField = searchForField,
        sinceApiVersion = sinceApiVersion
      )
    }
  }

  private fun toDtoFieldDescription(
    ref: String,
    refNullable: Boolean,
    refClass: TypeName?,
    refCollection: Boolean,
    fieldElement: KSPropertyDeclaration,
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

      refClass != null && refClass.toString() != "kotlin.Any" -> {
        if (refCollection) {
          Collection::class.generic(refClass.kotlin(refClass.isNullable))
        } else {
          refClass
        }
      }

      else -> fieldElement.type.resolve().toTypeName()
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
      entityFieldType = fieldElement.type.resolve().toTypeName(),
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
