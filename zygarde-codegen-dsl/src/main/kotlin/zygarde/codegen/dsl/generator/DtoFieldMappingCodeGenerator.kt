package zygarde.codegen.dsl.generator

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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.meta.Comment
import java.io.Serializable
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class DtoFieldMappingCodeGenerator(val dtoFieldMappings: Collection<DtoFieldMapping>) {

  val dtoPackageName = "zygarde.codegen.data.dto"
  val modelExtensionPackageName = "zygarde.codegen.model.extensions"
  var dtoToExtraToDtoMappingMap = dtoFieldMappings.filter { it.modelField.extra && it is DtoFieldMapping.ModelToDtoFieldMappingVo }.groupBy { it.dto }

  fun generateFileSpec(): Collection<FileSpec> {
    return listOf(
      generateDtos(),
      generateDtoExtraValue(),
      generateMapToDtoExtension(),
      generateApplyFromDtoExtension(),
    ).flatten()
  }

  private fun generateDtos(): List<FileSpec> {
    return dtoFieldMappings.groupBy { it.dto }.map { e ->
      val dto = e.key
      val mappings = e.value
      val dtoClassName = ClassName(dtoPackageName, dto.name)
      val dtoFileBuilder = FileSpec.builder(dtoClassName.packageName, dtoClassName.simpleName)
      val dtoClassBuilder = TypeSpec.classBuilder(dtoClassName)
        .addModifiers(KModifier.DATA)
        .addAnnotation(ApiModel::class)
        .addSuperinterface(Serializable::class)
      dto.superClass()?.also { superClass ->
        if (superClass.java.isInterface) {
          dtoClassBuilder.addSuperinterface(superClass)
        } else {
          dtoClassBuilder.superclass(superClass)
        }
      }

      val dtoConstructorBuilder = FunSpec.constructorBuilder()
      val fieldNameToInterfacePropertyMap = dto.superClass()?.takeIf { it.java.isInterface }?.memberProperties?.associateBy { it.name } ?: emptyMap()
      val fieldNameToMappingMap = mappings.associateBy { it.modelField.fieldName }

      listOf(fieldNameToInterfacePropertyMap.keys, fieldNameToMappingMap.keys).flatten().toSet().forEach { fieldName ->
        val memberFromSuperInterface = fieldNameToInterfacePropertyMap[fieldName]
        val mapping = fieldNameToMappingMap[fieldName]
        val fieldType = memberFromSuperInterface?.returnType?.asTypeName() ?: mapping?.fieldType() ?: return@forEach
        val comment = mapping?.comment ?: mapping?.modelField?.comment ?: memberFromSuperInterface?.findAnnotation<Comment>()?.comment
        dtoConstructorBuilder.addParameter(
          ParameterSpec
            .builder(fieldName, fieldType)
            .also {
              if (memberFromSuperInterface != null) {
                it.addModifiers(KModifier.OVERRIDE)
              }
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
                .addMember("notes=%S", comment.orEmpty())
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
    return dtoToExtraToDtoMappingMap
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
    return dtoFieldMappings
      .mapNotNull { if (it is DtoFieldMapping.ModelToDtoFieldMappingVo) it else null }
      .groupBy { it.modelField.modelClass }
      .map { e ->
        val modelClass = e.key
        val extensionClassName = "${modelClass.simpleName}ToDtoExtensions"
        val extensionFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, extensionClassName)
        val extensionClassBuilder = TypeSpec.objectBuilder(ClassName(modelExtensionPackageName, extensionClassName))

        e.value.groupBy { it.dto }.forEach { (dto, mappingsByDto) ->
          val codeBlockArgs = mutableListOf<Any>(ClassName(dtoPackageName, dto.name))
          val toDtoFuncParameters = mutableListOf<ParameterSpec>()

          if (mappingsByDto.any { it.modelField.extra }) {
            toDtoFuncParameters.add(
              ParameterSpec("extraValues", ClassName(dtoPackageName, "${dto.name}ExtraValues"))
            )
          }

          val dtoFieldSetterStatements = mappingsByDto.map { mapping ->
            val dtoFieldName = mapping.modelField.fieldName
            val modelFieldName = mapping.modelField.fieldName
            val dtoRef = mapping.dtoRef
            val q = if (mapping.modelField.fieldNullable) "?" else ""
            val valueProvider = mapping.valueProvider
            val valueProviderParameterType = mapping.valueProviderParameterType
            val isExtraField = mapping.modelField.extra
            if (valueProvider != null) {
              codeBlockArgs.add(valueProvider)
              val valueProviderParam = when (valueProviderParameterType) {
                ValueProviderParameterType.FIELD -> "this.$modelFieldName"
                ValueProviderParameterType.OBJECT -> "this"
              }

              "  $dtoFieldName = %T().getValue($valueProviderParam)"
            } else if (isExtraField) {
              "  $dtoFieldName = extraValues.$modelFieldName"
            } else if (dtoRef != null) {
              val modelForToDtoExtensions = mapping.modelField.fieldClass.simpleName
              codeBlockArgs.add(
                MemberName(
                  "$modelExtensionPackageName.${modelForToDtoExtensions}ToDtoExtensions",
                  "to${dtoRef.name}"
                )
              )
              if (mapping.refCollection) {
                "  $dtoFieldName = this.$modelFieldName$q.map{it.%M()}"
              } else {
                if (dtoToExtraToDtoMappingMap[dtoRef] != null) {
                  toDtoFuncParameters.add(
                    ParameterSpec(
                      "${modelFieldName}ExtraValues",
                      ClassName(dtoPackageName, "${dtoRef.name}ExtraValues")
                    )
                  )
                  "  $dtoFieldName = this.$modelFieldName$q.%M(${modelFieldName}ExtraValues)"
                } else {
                  "  $dtoFieldName = this.$modelFieldName$q.%M()"
                }
              }
            } else {
              "  $dtoFieldName = this.$modelFieldName"
            }
          }

          extensionClassBuilder.addFunction(
            FunSpec.builder("to${dto.name}")
              .receiver(modelClass)
              .also { fb ->
                toDtoFuncParameters.forEach {
                  fb.addParameter(it)
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

        extensionFileSpecBuilder
          .addType(extensionClassBuilder.build())
          .build()
      }
  }

  private fun generateApplyFromDtoExtension(): List<FileSpec> {
    return dtoFieldMappings
      .mapNotNull { if (it is DtoFieldMapping.ModelApplyFromDtoFieldMappingVo) it else null }
      .groupBy { it.modelField.modelClass }
      .map { e ->
        val modelClass = e.key
        val extensionClassName = "${modelClass.simpleName}ApplyValueExtensions"
        val extensionFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, extensionClassName)
        val extensionClassBuilder = TypeSpec.objectBuilder(ClassName(modelExtensionPackageName, extensionClassName))

        e.value.groupBy { it.dto }.forEach { dto, mappings ->
          val functionBuilder = FunSpec.builder("applyFrom")
            .addParameter("req", ClassName(dtoPackageName, dto.name))
            .receiver(modelClass)
            .returns(modelClass)

          mappings.forEach { mapping ->
            val modelFieldName = mapping.modelField.fieldName
            val dtoFieldName = mapping.modelField.fieldName
            val valueProvider = mapping.valueProvider
            if (valueProvider != null) {
              functionBuilder.addStatement(
                "this.$modelFieldName = %T().getValue(req.$dtoFieldName)",
                valueProvider
              )
            } else {
              functionBuilder.addStatement("this.$modelFieldName = req.$dtoFieldName")
            }
          }

          extensionClassBuilder.addFunction(
            functionBuilder.addStatement("return this").build()
          )
        }

        extensionFileSpecBuilder
          .addType(extensionClassBuilder.build())
          .build()
      }
  }

  private fun DtoFieldMapping.fieldType(): TypeName {
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
      .let { if (mapping.refCollection) Collection::class.generic(it) else it }
  }
}
