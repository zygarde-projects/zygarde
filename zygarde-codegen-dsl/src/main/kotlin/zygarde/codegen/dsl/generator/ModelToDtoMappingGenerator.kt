package zygarde.codegen.dsl.generator

import com.squareup.kotlinpoet.*
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import zygarde.codegen.dsl.model.internal.ModelToDtoFieldMappingVo
import java.io.Serializable

class ModelToDtoMappingGenerator {

  val dtoPackageName = "zygarde.codegen.data.dto"

  fun generateFileSpec(modelToDtoFieldMappingVoList: Collection<ModelToDtoFieldMappingVo>): Collection<FileSpec> {
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
        val fieldType = (
          mapping.dtoRefClass?.asTypeName()
            ?: mapping.dtoRef?.let { ClassName(dtoPackageName, it.name) }
            ?: mapping.modelField.fieldClass.asTypeName()
          )
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
}
