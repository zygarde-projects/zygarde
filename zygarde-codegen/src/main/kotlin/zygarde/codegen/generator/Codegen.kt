package zygarde.codegen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import zygarde.codegen.extension.kotlinpoet.toClassName
import zygarde.codegen.model.CodegenConfig
import zygarde.codegen.model.EntityToDtoMapping
import zygarde.codegen.model.FieldType
import zygarde.codegen.model.type.ValueProviderParameterType
import java.io.Serializable

class Codegen(val codegenConfig: CodegenConfig) {

  private val entityToDtoExtensionSpecBuilders: MutableMap<String, FileSpec.Builder> = mutableMapOf()
  private val dtoFileSpecBuilders: MutableMap<String, FileSpec.Builder> = mutableMapOf()
  private val dtoClassSpecBuilders: MutableMap<String, TypeSpec.Builder> = mutableMapOf()
  private val dtoConstructorSpecBuilders: MutableMap<String, FunSpec.Builder> = mutableMapOf()

  fun applyEntityToDtoMapping(mapping: EntityToDtoMapping) {
    val entityClassName = mapping.entityClassFullName.toClassName()
    val dtoClassName = mapping.dtoClassName.toClassName()
    val dtoPackageName = dtoClassName.packageName
    val extensionPackageName = "${codegenConfig.basePackageName}.entity.extensions"
    dtoFileSpecBuilders.getOrPut(
      dtoClassName.simpleName,
    ) {
      FileSpec.builder(dtoPackageName, dtoClassName.simpleName)
    }
    val dtoBuilder = dtoClassSpecBuilders.getOrPut(
      dtoClassName.simpleName
    ) {
      TypeSpec.classBuilder(dtoClassName)
        .addModifiers(KModifier.DATA)
        .addAnnotation(ApiModel::class)
        .addSuperinterface(Serializable::class)
    }
    val dtoConstructorBuilder = dtoConstructorSpecBuilders.getOrPut(
      dtoClassName.simpleName
    ) {
      FunSpec.constructorBuilder()
    }

    val extraValueFields = mutableListOf<FieldType>()
    val codeBlockArgs = mutableListOf<Any>(dtoClassName)
    val dtoFieldSetterStatements = mapping.fieldMappings
      .map { fieldMapping ->
        val entityFieldName = fieldMapping.entityField?.fieldName
        val dtoFieldName = fieldMapping.dtoField.fieldName
        val dtoFieldType = fieldMapping.dtoField.kotlinType()
        val propertySpecInDto = dtoBuilder.propertySpecs.find { p -> p.name == dtoFieldName }
        if (propertySpecInDto == null) {
          dtoConstructorBuilder.addParameter(
            ParameterSpec
              .builder(dtoFieldName, dtoFieldType)
              .also {
                if (fieldMapping.dtoField.nullable) {
                  it.defaultValue("null")
                }
              }
              .build()
          )
          dtoBuilder.addProperty(
            PropertySpec
              .builder(dtoFieldName, dtoFieldType)
              .initializer(dtoFieldName)
              .addAnnotation(
                AnnotationSpec.builder(ApiModelProperty::class)
                  .addMember("notes=%S", fieldMapping.comment)
                  .addMember("required=%L", !fieldMapping.dtoField.nullable)
                  .build()
              ).build()
          )
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

    val extraValuesName = "${dtoClassName.simpleName}ExtraValues"
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
        mapping.entityClassFullName
      ) { FileSpec.builder(extensionPackageName, "${entityClassName.simpleName}ToDtoExtensions") }

    builder.addFunction(
      FunSpec.builder("to${dtoClassName.simpleName}")
        .receiver(entityClassName)
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
}
