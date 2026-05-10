package zygarde.codegen.generator.shared

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.swagger.v3.oas.annotations.media.Schema
import zygarde.codegen.SearchType
import zygarde.data.jpa.search.EnhancedSearch
import java.io.Serializable

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

fun TypeName?.validValueProvider(): TypeName? {
  return if (this != null && !this.toString().contains("NoOpValueProvider")) {
    this
  } else {
    null
  }
}

fun AnnotationSpec.Builder.addSchemaRequiredMode(required: Boolean): AnnotationSpec.Builder {
  return addMember("requiredMode=%T.RequiredMode.%L", Schema::class, if (required) "REQUIRED" else "NOT_REQUIRED")
}

object ApiPropSharedGenerator {
  fun generateToDtoExtensionFunction(
    entityTypeName: TypeName,
    dtoPackageName: String,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val dtoClassName = ClassName(dtoPackageName, dtoName)
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

  fun generateApplyToEntityExtensionFunction(
    entityTypeName: TypeName,
    dtoPackageName: String,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
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

  fun generateSearchExtensionFunction(
    entityTypeName: TypeName,
    dtoPackageName: String,
    searchPackageName: String,
    dtoName: String,
    dtoFieldDescriptions: List<DtoFieldDescriptionVo>
  ): FunSpec {
    val dtoClass = ClassName(dtoPackageName, dtoName)
    val functionBuilder = FunSpec.builder("applyFrom$dtoName")
      .addParameter("req", dtoClass)
      .receiver(EnhancedSearch::class.asClassName().parameterizedBy(entityTypeName))

    dtoFieldDescriptions
      .forEach {
        val searchForField = it.searchForField ?: it.entityFieldName
        val fieldName = it.dtoFieldName
        val fieldExtensionMember = MemberName(searchPackageName, searchForField)
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

  /**
   * Builds DTO TypeSpecs and extension FileSpec from grouped field descriptions.
   * Returns a pair of (dtoName -> TypeSpec map, extension FileSpec.Builder).
   * Callers are responsible for writing the files using their framework's I/O mechanism.
   */
  fun buildDtoClassesAndExtensions(
    entityTypeName: TypeName,
    dtoPackageName: String,
    searchPackageName: String,
    dtoExtensionName: String,
    allDescriptions: List<DtoFieldDescriptionVo>,
    dtoInheritMap: Map<String, TypeName>
  ): Pair<Map<String, TypeSpec>, FileSpec.Builder> {
    val dtoSpecs = mutableMapOf<String, TypeSpec>()
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
            generateToDtoExtensionFunction(
              entityTypeName,
              dtoPackageName,
              dtoName,
              dtoFieldDescriptions.filter { it.generateToDtoExtension }
            )
          )
        }

        if (dtoFieldDescriptions.any { it.generateApplyToEntityExtension }) {
          fileBuilderForExtension.addFunction(
            generateApplyToEntityExtensionFunction(
              entityTypeName,
              dtoPackageName,
              dtoName,
              dtoFieldDescriptions.filter { it.generateApplyToEntityExtension }
            )
          )
        }

        val isSearchDto = dtoFieldDescriptions.any { it.searchType != SearchType.NONE }
        if (isSearchDto) {
          fileBuilderForExtension.addFunction(
            generateSearchExtensionFunction(
              entityTypeName,
              dtoPackageName,
              searchPackageName,
              dtoName,
              dtoFieldDescriptions.filter { it.searchType != SearchType.NONE }
            )
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
                .addSchemaRequiredMode(!fieldType.isNullable)
                .build()
            ).build().also { dtoBuilder.addProperty(it) }
        }

        dtoSpecs[dtoName] = dtoBuilder.primaryConstructor(constructorBuilder.build()).build()
      }

    return dtoSpecs to fileBuilderForExtension
  }
}
