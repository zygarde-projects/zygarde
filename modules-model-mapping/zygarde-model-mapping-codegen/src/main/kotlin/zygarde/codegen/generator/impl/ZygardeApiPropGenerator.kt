package zygarde.codegen.generator.impl

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
import com.squareup.kotlinpoet.asTypeName
import io.swagger.v3.oas.annotations.media.Schema
import zygarde.codegen.AdditionalDtoProps
import zygarde.codegen.ApiProp
import zygarde.codegen.DtoInherits
import zygarde.codegen.SearchType
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.isNullable
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.nullableTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.value.NoOpValueProvider
import zygarde.data.jpa.search.EnhancedSearch
import java.io.Serializable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.persistence.Transient

class ZygardeApiPropGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  data class DtoFieldDescriptionVo(
    val entityFieldName: String,
    val fieldType: TypeName,
    val dtoName: String,
    val dtoFieldName: String,
    val comment: String,
    val dtoRef: String = "",
    val dtoRefCollection: Boolean = false,
    val valueProvider: TypeName? = null,
    val entityValueProvider: TypeName? = null,
    val generateToDtoExtension: Boolean = false,
    val generateApplyToEntityExtension: Boolean = false,
    val searchType: SearchType = SearchType.NONE,
    val searchForField: String? = null
  )

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
                fieldType = safeGetTypeFromAnnotation { additionalDtoProp.fieldType.asTypeName() },
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
      .flatMap { elem ->
        val isTransient = elem.getAnnotation(Transient::class.java) != null
        elem.getAnnotationsByType(ApiProp::class.java)
          .flatMap { apiProp ->
            listOf(
              apiProp.dto.flatMap { dto ->
                val refClass = safeGetTypeFromAnnotation { dto.refClass.asTypeName() }.kotlin(dto.refClassNullable)
                val valueProvider = safeGetTypeFromAnnotation { dto.valueProvider.asTypeName() }.kotlin(false).validValueProvider()
                val entityValueProvider = safeGetTypeFromAnnotation { dto.entityValueProvider.asTypeName() }.kotlin(false).validValueProvider()
                dto.names.plus(dto.name).filter { it.isNotEmpty() }.map { dtoName ->
                  toDtoFieldDescription(
                    elem = elem,
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
              apiProp.requestDto.flatMap { dto ->
                val refClass = safeGetTypeFromAnnotation { dto.refClass.asTypeName() }.kotlin(dto.refClassNullable)
                val valueProvider = safeGetTypeFromAnnotation { dto.valueProvider.asTypeName() }.kotlin(false).validValueProvider()
                dto.names.plus(dto.name).filter { it.isNotEmpty() }.map { dtoName ->
                  toDtoFieldDescription(
                    elem = elem,
                    ref = dto.ref,
                    refNullable = dto.refNullable,
                    refClass = refClass,
                    refCollection = dto.refCollection,
                    dtoName = dtoName,
                    dtoFieldName = dto.fieldName,
                    comment = apiProp.comment,
                    valueProvider = valueProvider,
                    forceNotNull = dto.notNullInReq,
                    forceNullable = dto.forceNullableInReq,
                    isTransient = isTransient
                  ).copy(
                    generateToDtoExtension = false,
                    generateApplyToEntityExtension = !isTransient && dto.applyValueToEntity && dto.searchType == SearchType.NONE,
                    searchType = dto.searchType,
                    searchForField = dto.searchForField.takeIf { it.isNotEmpty() }
                  )
                }
              }
            ).flatten()
          }
          .toMutableList()
      }
    val allDescriptions = listOf(dtoDescriptionsFromAdditionalDtoProps, dtoDescriptionsFromElementFields).flatten()
    if (allDescriptions.isEmpty()) return

    val dtoExtensionName = "${element.name()}DtoExtensions"
    val fileBuilderForExtension = FileSpec.builder(dtoPackageName, dtoExtensionName)

    allDescriptions.groupBy { it.dtoName }
      .forEach { (dtoName, dtoFieldDescriptions) ->
        val dtoBuilder = TypeSpec.classBuilder(dtoName)
          .addModifiers(KModifier.DATA)
          .addAnnotation(Schema::class)
          .addSuperinterface(Serializable::class)

        dtoInheritMap.get(dtoName)?.let(dtoBuilder::superclass)

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
          val fieldType = dto.fieldType.let { if (isSearchDto) it.copy(nullable = true) else it }
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
          .writeTo(folderToGenerate())
      }

    fileBuilderForExtension.build().writeTo(folderToGenerate())
  }

  private fun toDtoFieldDescription(
    ref: String,
    refNullable: Boolean,
    refClass: TypeName,
    refCollection: Boolean,
    elem: Element,
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
      refClass.toString() != "java.lang.Object" -> {
        if (refCollection) {
          Collection::class.generic(refClass.kotlin(refClass.isNullable))
        } else {
          refClass
        }
      }
      else -> elem.nullableTypeName()
    }
    val entityFieldName = elem.fieldName()
    if (isTransient && !entityFieldName.startsWith("_")) {
      throw IllegalArgumentException("transient field '$entityFieldName' should be starts with '_'")
    }
    if (!isTransient && entityFieldName.startsWith("_")) {
      throw IllegalArgumentException("field '$entityFieldName' should be annotated with @Transient")
    }
    return DtoFieldDescriptionVo(
      entityFieldName = entityFieldName,
      fieldType = fieldType.kotlin(canBeNullable = if (forceNullable) true else !forceNotNull && elem.isNullable()),
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
    return if (this != null && this.toString() != NoOpValueProvider::class.asTypeName().toString()) {
      this
    } else {
      null
    }
  }

  private fun generateToDtoExtensionFunction(
    element: Element,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val codeBlockArgs = mutableListOf<Any>(ClassName(dtoPackageName, dtoName))
    val dtoFieldSetterStatements = dtoFieldDescriptions
      .map {
        val q = if (it.fieldType.isNullable) "?" else ""
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
      .receiver(element.typeName())
      .addStatement(
        """return %T(
${dtoFieldSetterStatements.joinToString(",\r\n")}              
)""".trimMargin(),
        *codeBlockArgs.toTypedArray()
      )
      .build()
  }

  private fun generateApplyToEntityExtensionFunction(
    element: Element,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val functionBuilder = FunSpec.builder("applyFrom$dtoName")
      .addParameter("req", ClassName(dtoPackageName, dtoName))
      .receiver(element.typeName())
      .returns(element.typeName())

    dtoFieldDescriptions
      .forEach {
        if (it.valueProvider != null) {
          val q = if (it.fieldType.isNullable) "?" else ""
          functionBuilder.addStatement(
            "this.${it.entityFieldName} = req.${it.dtoFieldName}$q.let{ %T().getValue(it) }",
            it.valueProvider
          )
        } else {
          functionBuilder.addStatement("this.${it.entityFieldName} = req.${it.dtoFieldName}")
        }
      }

    return functionBuilder.addStatement("return this").build()
  }

  private fun generateSearchExtensionFunction(
    element: Element,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val dtoClass = ClassName(dtoPackageName, dtoName)
    val functionBuilder = FunSpec.builder("applyFrom$dtoName")
      .addParameter("req", dtoClass)
      .receiver(EnhancedSearch::class.generic(element.typeName()))

    dtoFieldDescriptions
      .forEach {
        val searchForField = it.searchForField ?: it.entityFieldName
        val fieldName = it.dtoFieldName
        val fieldExtensionMember = MemberName(
          packageName(processingEnv.options.getOrDefault(ZygardeKaptOptions.ENTITY_PACKAGE_SEARCH, "entity.search")),
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
