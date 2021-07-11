package zygarde.codegen.dsl.generator

import com.squareup.kotlinpoet.*
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import zygarde.codegen.dsl.model.internal.ModelToDtoFieldMappingVo
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import java.io.Serializable

class ModelToDtoMappingGenerator(val modelToDtoFieldMappingVoList: Collection<ModelToDtoFieldMappingVo>) {

  val dtoPackageName = "zygarde.codegen.data.dto"
  val modelExtensionPackageName = "zygarde.codegen.model.extensions"

  fun generateFileSpec(): Collection<FileSpec> {
    return listOf(
      generateDtos(),
      generateDtoExtraValue(),
      generateMapToDtoExtension(),
    ).flatten()
  }

  private fun generateDtos(): List<FileSpec> {
    return modelToDtoFieldMappingVoList.groupBy { it.dto }.map { e ->
      val dto = e.key
      val mappings = e.value
      val dtoClassName = ClassName(dtoPackageName, dto.name)
      val dtoFileBuilder = FileSpec.builder(dtoClassName.packageName, dtoClassName.simpleName)
      val dtoClassBuilder = TypeSpec.classBuilder(dtoClassName)
        .addModifiers(KModifier.DATA)
        .addAnnotation(ApiModel::class)
        .addSuperinterface(Serializable::class)
      dto.superClass()?.let(dtoClassBuilder::superclass)
      // TODO process super dto
      val dtoConstructorBuilder = FunSpec.constructorBuilder()
      mappings.associateBy { it.modelField.fieldName }.forEach { fieldName, mapping ->
        val fieldType = mapping.fieldType()
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
                .addMember("notes=%S", mapping.comment)
                .addMember("required=%L", !fieldType.isNullable)
                .build()
            ).build()
        )
      }

      dtoFileBuilder
        .addType(
          dtoClassBuilder
            .primaryConstructor(dtoConstructorBuilder.build())
            .build()
        )
        .build()
    }
  }

  private fun generateDtoExtraValue(): List<FileSpec> {
    return modelToDtoFieldMappingVoList.filter { it.modelField.extra }.groupBy { it.dto }
      .map { e ->
        val dto = e.key
        val mappings = e.value
        val extraValuesName = "${dto.name}ExtraValues"
        val extraValueClass = ClassName(dtoPackageName, extraValuesName)
        val extraValueClassConstructorBuilder = FunSpec.constructorBuilder()
        val extraValueClassBuilder = TypeSpec.classBuilder(extraValueClass)
          .addModifiers(KModifier.DATA)
          .addSuperinterface(Serializable::class)

        mappings.forEach { mapping ->
          val fieldName = mapping.modelField.fieldName
          extraValueClassConstructorBuilder.addParameter(
            ParameterSpec
              .builder(fieldName, mapping.fieldType())
              .build()
          )
          extraValueClassBuilder.addProperty(
            PropertySpec
              .builder(fieldName, mapping.fieldType())
              .initializer(fieldName)
              .build()
          )
        }

        extraValueClassBuilder
          .primaryConstructor(extraValueClassConstructorBuilder.build())

        FileSpec.builder(dtoPackageName, extraValuesName)
          .addType(extraValueClassBuilder.build())
          .build()
      }
  }

  private fun generateMapToDtoExtension(): List<FileSpec> {
    return modelToDtoFieldMappingVoList.groupBy { it.modelField.modelClass }
      .map { e ->
        val modelClass = e.key
        val extensionFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, "${modelClass.simpleName}ToDtoExtensions")

        e.value.groupBy { it.dto }.forEach { (dto, mappingsByDto) ->
          val codeBlockArgs = mutableListOf<Any>(ClassName(dtoPackageName, dto.name))
          val dtoFieldSetterStatements = mappingsByDto.map { mapping ->
            val dtoFieldName = mapping.modelField.fieldName
            val modelFieldName = mapping.modelField.fieldName
            val dtoRef = mapping.dtoRef
            val q = if (mapping.modelField.fieldNullable) "?" else ""
            val valueProvider = mapping.valueProvider
            val isExtraField = mapping.modelField.extra
            if (valueProvider != null) {
              codeBlockArgs.add(valueProvider)
              val valueProviderParam = when (mapping.valueProviderParameterType) {
                ValueProviderParameterType.FIELD -> "this.$modelFieldName"
                ValueProviderParameterType.OBJECT -> "this"
              }

              "  $dtoFieldName = %T().getValue($valueProviderParam)"
            } else if (dtoRef != null) {
              codeBlockArgs.add(MemberName(modelExtensionPackageName, "to${dtoRef.name}"))
              if (mapping.refCollection) {
                "  $dtoFieldName = this.$modelFieldName$q.map{it.%M()}"
              } else {
                "  $dtoFieldName = this.$modelFieldName$q.%M()"
              }
            } else if (isExtraField) {
              "  $dtoFieldName = extraValues.$modelFieldName"
            } else {
              "  $dtoFieldName = this.$modelFieldName"
            }
          }

          extensionFileSpecBuilder.addFunction(
            FunSpec.builder("to${dto.name}")
              .receiver(modelClass)
              .also {
                if (mappingsByDto.any { it.modelField.extra }) {
                  it.addParameter("extraValues", ClassName(dtoPackageName, "${dto.name}ExtraValues"))
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

        extensionFileSpecBuilder.build()
      }
  }

  private fun ModelToDtoFieldMappingVo.fieldType(): TypeName {
    val mapping = this
    val fieldTypeNullable = when (mapping.forceNull) {
      ForceNull.NONE -> mapping.modelField.fieldNullable
      ForceNull.NULL -> true
      ForceNull.NOT_NULL -> false
    }
    return (
      mapping.dtoRefClass
        ?.generic(*mapping.modelField.genericClasses)
        ?: mapping.dtoRef?.let { ClassName(dtoPackageName, it.name) }
        ?: mapping.modelField.fieldClass.generic(*mapping.modelField.genericClasses)
      ).kotlin(fieldTypeNullable)
  }
}