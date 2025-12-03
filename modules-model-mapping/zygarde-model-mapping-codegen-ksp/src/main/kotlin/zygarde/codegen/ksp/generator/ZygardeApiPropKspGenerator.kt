package zygarde.codegen.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.swagger.v3.oas.annotations.media.Schema
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
import zygarde.data.jpa.search.EnhancedSearch
import java.io.Serializable

class ZygardeApiPropKspGenerator(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String>
) {
  data class DtoFieldDescriptionVo(
    val entityFieldName: String,
    val entityFieldType: TypeName,
    val dtoName: String,
    val dtoFieldName: String,
    val dtoFieldType: TypeName,
    val comment: String,
    val dtoRef: String = "",
    val dtoRefCollection: Boolean = false,
    val valueProvider: TypeName? = null,
    val entityValueProvider: TypeName? = null,
    val generateToDtoExtension: Boolean = false,
    val generateApplyToEntityExtension: Boolean = false,
    val searchType: SearchType = SearchType.NONE,
    val searchForField: String? = null,
    val sinceApiVersion: Long = 0,
  )

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

    val dtoExtensionName = "${element.name()}DtoExtensions"
    val fileBuilderForExtension = FileSpec.builder(dtoPackageName, dtoExtensionName)

    allDescriptions.groupBy { it.dtoName }
      .forEach { (dtoName, dtoFieldDescriptions) ->
        val dtoBuilder = TypeSpec.classBuilder(dtoName)
          .addModifiers(KModifier.DATA)
          .addAnnotation(Schema::class)
          .addSuperinterface(Serializable::class)

        dtoInheritMap[dtoName]?.let(dtoBuilder::superclass)

        val constructorBuilder = FunSpec.constructorBuilder()

        if (dtoFieldDescriptions.any { it.generateToDtoExtension }) {
          fileBuilderForExtension.addFunction(
            generateToDtoExtensionFunction(element, dtoName, dtoFieldDescriptions.filter { it.generateToDtoExtension })
          )
        }

        if (dtoFieldDescriptions.any { it.generateApplyToEntityExtension }) {
          fileBuilderForExtension.addFunction(
            generateApplyToEntityExtensionFunction(element, dtoName, dtoFieldDescriptions.filter { it.generateApplyToEntityExtension })
          )
        }

        val isSearchDto = dtoFieldDescriptions.any { it.searchType != SearchType.NONE }
        if (isSearchDto) {
          fileBuilderForExtension.addFunction(
            generateSearchExtensionFunction(element, dtoName, dtoFieldDescriptions.filter { it.searchType != SearchType.NONE })
          )
        }

        dtoFieldDescriptions.forEach { dto ->
          val fieldName = dto.dtoFieldName
          val fieldType = dto.dtoFieldType.let { if (isSearchDto) it.copy(nullable = true) else it }
          ParameterSpec
            .builder(fieldName, fieldType)
            .also {
              if (isSearchDto || fieldType.isNullable) {
                it.defaultValue("null")
              }
            }
            .build().also { constructorBuilder.addParameter(it) }
          PropertySpec
            .builder(fieldName, fieldType)
            .mutable(true)
            .initializer(fieldName)
            .addAnnotation(
              AnnotationSpec.builder(Schema::class)
                .addMember("description=%S", dto.comment)
                .addMember("required=%L", !fieldType.isNullable)
                .build()
            ).build().also { dtoBuilder.addProperty(it) }
        }

        FileSpec.builder(dtoPackageName, dtoName)
          .addType(
            dtoBuilder.primaryConstructor(constructorBuilder.build()).build()
          )
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

  private fun TypeName?.validValueProvider(): TypeName? {
    return if (this != null && !this.toString().contains("NoOpValueProvider")) {
      this
    } else {
      null
    }
  }

  private fun generateToDtoExtensionFunction(
    element: KSClassDeclaration,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val dtoClassName = ClassName(dtoPackageName, dtoName)
    val entityTypeName = element.asType(emptyList()).toTypeName().copy(nullable = false)
    val codeBlockArgs = mutableListOf<Any>(dtoClassName)
    val dtoFieldSetterStatements = dtoFieldDescriptions
      .map {
        val q = if (it.dtoFieldType.isNullable) "?" else ""
        if (it.entityValueProvider != null) {
          codeBlockArgs.add(it.entityValueProvider)
          "  ${it.dtoFieldName} = %T().getValue(this)"
        } else if (it.valueProvider != null) {
          codeBlockArgs.add(it.valueProvider)
          "  ${it.dtoFieldName} = this.${it.entityFieldName}$q.let{ %T().getValue(it) } "
        } else if (it.dtoRef.isNotEmpty()) {
          codeBlockArgs.add(MemberName(dtoPackageName, "to${it.dtoRef}"))
          if (it.dtoRefCollection) {
            "  ${it.dtoFieldName} = this.${it.entityFieldName}$q.map{it.%M()}"
          } else {
            "  ${it.dtoFieldName} = this.${it.entityFieldName}$q.%M()"
          }
        } else {
          "  ${it.dtoFieldName} = this.${it.entityFieldName}"
        }
      }
    return FunSpec.builder("to$dtoName")
      .receiver(entityTypeName)
      .returns(dtoClassName)
      .addStatement(
        """return %T(
${dtoFieldSetterStatements.joinToString(",\r\n")}              
)
        """.trimMargin(),
        *codeBlockArgs.toTypedArray()
      )
      .build()
  }

  private fun generateApplyToEntityExtensionFunction(
    element: KSClassDeclaration,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val entityTypeName = element.asType(emptyList()).toTypeName().copy(nullable = false)
    val functionBuilder = FunSpec.builder("applyFrom$dtoName")
      .addParameter("req", ClassName(dtoPackageName, dtoName))
      .receiver(entityTypeName)
      .returns(entityTypeName)

    val checkApiVersion = dtoFieldDescriptions.any { it.sinceApiVersion > 0 }
    if (checkApiVersion) {
      functionBuilder.addStatement("val apiVersion = %T.version()", ClassName("zygarde.ctx", "ApiVersionContext"))
    }

    dtoFieldDescriptions
      .groupBy { it.sinceApiVersion }
      .forEach { (sinceApiVersion, fieldDescriptionVos) ->
        var indent = ""
        if (sinceApiVersion > 0) {
          functionBuilder.addStatement("if(apiVersion >= $sinceApiVersion){")
          indent = "  "
        }
        fieldDescriptionVos.forEach { fieldDescriptionVo ->
          if (fieldDescriptionVo.valueProvider != null) {
            val q = if (fieldDescriptionVo.dtoFieldType.isNullable) "?" else ""
            functionBuilder.addStatement(
              indent + "this.${fieldDescriptionVo.entityFieldName} = req.${fieldDescriptionVo.dtoFieldName}$q.let{ %T().getValue(it) }",
              fieldDescriptionVo.valueProvider
            )
          } else {
            if (fieldDescriptionVo.dtoFieldType.isNullable && !fieldDescriptionVo.entityFieldType.isNullable) {
              functionBuilder.addStatement(indent + "req.${fieldDescriptionVo.dtoFieldName}?.let{ this.${fieldDescriptionVo.entityFieldName} = it }")
            } else {
              functionBuilder.addStatement(indent + "this.${fieldDescriptionVo.entityFieldName} = req.${fieldDescriptionVo.dtoFieldName}")
            }
          }
        }
        if (sinceApiVersion > 0) {
          functionBuilder.addStatement("}")
        }
      }

    return functionBuilder.addStatement("return this").build()
  }

  private fun generateSearchExtensionFunction(
    element: KSClassDeclaration,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val entityTypeName = element.asType(emptyList()).toTypeName().copy(nullable = false)
    val dtoClass = ClassName(dtoPackageName, dtoName)
    val functionBuilder = FunSpec.builder("applyFrom$dtoName")
      .addParameter("req", dtoClass)
      .receiver(EnhancedSearch::class.generic(entityTypeName))

    dtoFieldDescriptions
      .forEach {
        val searchForField = it.searchForField ?: it.entityFieldName
        val fieldName = it.dtoFieldName
        val fieldExtensionMember = MemberName(
          packageName(options.getOrDefault(ENTITY_PACKAGE_SEARCH, "entity.search")),
          searchForField
        )
        when (it.searchType) {
          SearchType.EQ -> functionBuilder.addStatement("%M() eq req.$fieldName", fieldExtensionMember)
          SearchType.NOT_EQ -> functionBuilder.addStatement("%M() ne req.$fieldName", fieldExtensionMember)
          SearchType.LT -> functionBuilder.addStatement("%M() lt req.$fieldName", fieldExtensionMember)
          SearchType.GT -> functionBuilder.addStatement("%M() gt req.$fieldName", fieldExtensionMember)
          SearchType.LTE -> functionBuilder.addStatement("%M() lte req.$fieldName", fieldExtensionMember)
          SearchType.GTE -> functionBuilder.addStatement("%M() gte req.$fieldName", fieldExtensionMember)
          SearchType.IN_LIST -> functionBuilder.addStatement("%M() inList req.$fieldName", fieldExtensionMember)
          SearchType.KEYWORD -> functionBuilder.addStatement("%M() keyword req.$fieldName", fieldExtensionMember)
          SearchType.STARTS_WITH -> functionBuilder.addStatement("%M() startsWith req.$fieldName", fieldExtensionMember)
          SearchType.ENDS_WITH -> functionBuilder.addStatement("%M() endsWith req.$fieldName", fieldExtensionMember)
          SearchType.CONTAINS -> functionBuilder.addStatement("%M() contains req.$fieldName", fieldExtensionMember)
          SearchType.LIST_CONTAINS_ANY -> functionBuilder.addStatement("%M() containsAny req.$fieldName", fieldExtensionMember)
          SearchType.DATE_RANGE -> functionBuilder.addStatement(
            "%M() %M req.$fieldName",
            fieldExtensionMember,
            MemberName("zygarde.data.jpa.search.action", "dateRange")
          )

          SearchType.DATE_TIME_RANGE -> functionBuilder.addStatement(
            "%M() %M req.$fieldName",
            fieldExtensionMember,
            MemberName("zygarde.data.jpa.search.action", "dateTimeRange")
          )

          else -> {
          }
        }
      }

    return functionBuilder.build()
  }
}
