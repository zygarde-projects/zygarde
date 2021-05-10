package zygarde.codegen.generator

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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.extension.kotlinpoet.toClassName
import zygarde.codegen.model.CodegenConfig
import zygarde.codegen.model.EntityToDtoMapping
import zygarde.codegen.model.FieldType
import zygarde.codegen.model.internal.DtoBuilders
import zygarde.codegen.model.internal.DtoExtraField
import zygarde.codegen.model.type.ValueProviderParameterType
import java.io.Serializable

class Codegen(val config: CodegenConfig) {

  private val entityToDtoExtensionSpecBuilders: MutableMap<String, FileSpec.Builder> = mutableMapOf()
  private val dtoFileSpecBuilders: MutableMap<String, FileSpec.Builder> = mutableMapOf()
  private val dtoClassSpecBuilders: MutableMap<String, TypeSpec.Builder> = mutableMapOf()
  private val dtoConstructorSpecBuilders: MutableMap<String, FunSpec.Builder> = mutableMapOf()
  private val dtoExtraFields: MutableMap<String, MutableList<DtoExtraField>> = mutableMapOf()

  fun getOrAddDtoBuilders(dtoClass: String, dtoSuperClass: String? = null): DtoBuilders {
    val dtoType = dtoClass.toClassName()
    val dtoPackageName = dtoType.packageName
    val dtoFileBuilder = dtoFileSpecBuilders.getOrPut(
      dtoType.simpleName,
    ) {
      FileSpec.builder(dtoPackageName, dtoType.simpleName)
    }
    val dtoClassBuilder = dtoClassSpecBuilders.getOrPut(
      dtoType.simpleName
    ) {
      TypeSpec.classBuilder(dtoType)
        .addModifiers(KModifier.DATA)
        .addAnnotation(ApiModel::class)
        .addSuperinterface(Serializable::class)
    }

    dtoSuperClass?.toClassName()?.let(dtoClassBuilder::superclass)

    val dtoConstructorBuilder = dtoConstructorSpecBuilders.getOrPut(
      dtoType.simpleName
    ) {
      FunSpec.constructorBuilder()
    }
    return DtoBuilders(dtoFileBuilder, dtoClassBuilder, dtoConstructorBuilder)
  }

  fun applyEntityToDtoMapping(mapping: EntityToDtoMapping) {
    val entityType = mapping.entityClass.toClassName()
    val dtoType = mapping.dtoClass.toClassName()
    val dtoPackageName = dtoType.packageName
    val (_, dtoClassBuilder) = getOrAddDtoBuilders(mapping.dtoClass)
    val extensionPackageName = "${config.basePackageName}.entity.extensions"
    val extraValueFields = mutableListOf<FieldType>()
    val codeBlockArgs = mutableListOf<Any>(dtoType)
    val dtoFieldSetterStatements = mapping.fieldMappings
      .map { fieldMapping ->
        val entityFieldName = fieldMapping.entityField?.fieldName
        val dtoFieldName = fieldMapping.dtoField.fieldName
        val dtoFieldType = fieldMapping.dtoField.kotlinType()
        val propertySpecInDto = dtoClassBuilder.propertySpecs.find { p -> p.name == dtoFieldName }
        if (propertySpecInDto == null) {
          addFieldToDto(mapping.dtoClass, dtoFieldName, dtoFieldType, fieldMapping.comment)
        }

        val isExtraCollectValue = fieldMapping.entityField == null && fieldMapping.valueProviderClassName == null
        val q = if (fieldMapping.entityField?.nullable == true) "?" else ""
        if (fieldMapping.valueProviderClassName != null) {
          val valueProvider = fieldMapping.valueProviderClassName.toClassName()
          val valueProviderParam = if (fieldMapping.valueProviderParameterType == ValueProviderParameterType.FIELD) {
            "this.$entityFieldName"
          } else {
            "this"
          }
          codeBlockArgs.add(valueProvider)
          "  $dtoFieldName = %T().getValue($valueProviderParam)"
        } else if (fieldMapping.dtoRef != null) {
          codeBlockArgs.add(MemberName(extensionPackageName, "to${fieldMapping.dtoRef}"))
          if (fieldMapping.dtoRefCollection) {
            "  $dtoFieldName = this.$entityFieldName$q.map{it.%M()}"
          } else {
            "  $dtoFieldName = this.$entityFieldName$q.%M()"
          }
        } else if (isExtraCollectValue) {
          extraValueFields.add(fieldMapping.dtoField)
          "  $dtoFieldName = extraValues.$dtoFieldName"
        } else {
          "  $dtoFieldName = this.$entityFieldName"
        }
      }

    val extraValuesName = "${dtoType.simpleName}ExtraValues"
    val extraValueClass = ClassName(dtoPackageName, extraValuesName)
    if (extraValueFields.isNotEmpty()) {
      dtoFileSpecBuilders.getOrPut(
        extraValuesName
      ) {
        FileSpec.builder(dtoPackageName, extraValuesName)
      }

      val extraValueClassBuilder = dtoClassSpecBuilders.getOrPut(
        extraValuesName
      ) {
        TypeSpec.classBuilder(extraValueClass)
          .addModifiers(KModifier.DATA)
          .addAnnotation(ApiModel::class)
          .addSuperinterface(Serializable::class)
      }
      val extraValueClassConstructorBuilder = dtoConstructorSpecBuilders.getOrPut(
        extraValuesName
      ) {
        FunSpec.constructorBuilder()
      }

      extraValueFields.forEach { ev ->
        extraValueClassConstructorBuilder.addParameter(
          ParameterSpec
            .builder(ev.fieldName, ev.kotlinType())
            .build()
        )
        extraValueClassBuilder.addProperty(
          PropertySpec
            .builder(ev.fieldName, ev.kotlinType())
            .initializer(ev.fieldName)
            .build()
        )
      }
    }

    val builder = entityToDtoExtensionSpecBuilders
      .getOrPut(
        mapping.entityClass
      ) { FileSpec.builder(extensionPackageName, "${entityType.simpleName}ToDtoExtensions") }

    builder.addFunction(
      FunSpec.builder("to${dtoType.simpleName}")
        .receiver(entityType)
        .also {
          if (extraValueFields.isNotEmpty()) {
            it.addParameter("extraValues", extraValueClass)
          }
        }
        .addStatement(
          """return %T(
${dtoFieldSetterStatements.joinToString(",\r\n")}              
)""".trimMargin(),
          *codeBlockArgs.toTypedArray()
        )
        .build()
    )
  }

  fun addExtraFieldToDto(dtoClass: String, fieldName: String, fieldClass: String, comment: String?, nullable: Boolean) {
    dtoExtraFields.getOrPut(dtoClass) { mutableListOf() }.add(DtoExtraField(fieldName, fieldClass, comment))
    addFieldToDto(dtoClass, fieldName, fieldClass.toClassName().kotlin(nullable), comment.orEmpty())
  }

  fun allFileSpecs(): List<FileSpec> {
    return listOf(
      entityToDtoExtensionSpecBuilders.values.map { it.build() },
      dtoFileSpecBuilders.map { e ->
        dtoClassSpecBuilders[e.key]
          ?.also {
            dtoConstructorSpecBuilders[e.key]?.build()?.let(it::primaryConstructor)
          }
          ?.build()
          ?.let(e.value::addType)
        e.value.build()
      }
    ).flatten()
  }

  private fun addFieldToDto(dtoClass: String, fieldName: String, fieldType: TypeName, comment: String) {
    val (_, dtoClassBuilder, dtoConstructorBuilder) = getOrAddDtoBuilders(dtoClass)
    dtoConstructorBuilder.addParameter(
      ParameterSpec
        .builder(fieldName, fieldType)
        .also {
          if (fieldType.isNullable) {
            it.defaultValue("null")
          }
        }
        .build()
    )
    dtoClassBuilder.addProperty(
      PropertySpec
        .builder(fieldName, fieldType)
        .initializer(fieldName)
        .addAnnotation(
          AnnotationSpec.builder(ApiModelProperty::class)
            .addMember("notes=%S", comment)
            .addMember("required=%L", !fieldType.isNullable)
            .build()
        ).build()
    )
  }
}
