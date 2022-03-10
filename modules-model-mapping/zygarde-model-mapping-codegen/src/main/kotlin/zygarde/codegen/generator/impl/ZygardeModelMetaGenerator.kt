package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.springframework.util.FileSystemUtils
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZygardeKaptOptions.Companion.MODEL_META_GENERATE_PACKAGE
import zygarde.codegen.dsl.ModelMappingDslCodegen
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.meta.Comment
import zygarde.codegen.meta.ModelMetaField
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeModelMetaGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  val targetFolder by lazy {
    folderToGenerate(ZygardeKaptOptions.MODEL_META_GENERATE_TARGET)
  }

  fun generateModelMeta(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    FileSystemUtils.deleteRecursively(targetFolder)
    elements.forEach {
      generateMetaFields(it)
    }
  }

  private val starType = TypeVariableName("*")

  private fun generateMetaFields(modelElement: Element) {
    val className = modelElement.simpleName.toString()
    val pack = packageName(processingEnv.options.getOrDefault(MODEL_META_GENERATE_PACKAGE, "model.meta"))
    val fileNameForFields = "Abstract${className}Codegen"
    val classBuilder = TypeSpec.classBuilder(fileNameForFields)
      .addModifiers(KModifier.ABSTRACT)
      .superclass(ModelMappingDslCodegen::class.generic(modelElement.typeName()))
      .addSuperclassConstructorParameter("%T::class", modelElement.typeName())

    val allFieldsIncludeSuper = modelElement.allFieldsIncludeSuper()
    allFieldsIncludeSuper.forEach { field ->
      classBuilder.addProperty(
        field.buildMetaProperty(modelElement)
      )
      classBuilder.addFunction(
        field.buildMetaDslFn(modelElement)
      )
    }

    val allFieldNames = allFieldsIncludeSuper.map { it.fieldName() }

    classBuilder.addProperty(
      PropertySpec
        .builder(
          "allFields",
          Collection::class.generic(
            ModelMetaField::class.generic(
              modelElement.typeName(),
              starType
            )
          )
        )
        .initializer(
          "listOf(${allFieldNames.joinToString(",")})"
        )
        .build()
    )

    classBuilder.addFunction(
      FunSpec
        .builder("mapAllFields")
        .addParameter(
          ParameterSpec(
            "dsl",
            LambdaTypeName.get(
              receiver = ModelMetaField::class.asClassName().parameterizedBy(
                modelElement.typeName(),
                starType,
              ),
              returnType = Unit::class.asTypeName()
            )
          )
        )
        .addCode("allFields.forEach{ dsl.invoke(it) }")
        .build()
    )

    FileSpec.builder(pack, fileNameForFields)
      .addType(classBuilder.build())
      .build()
      .writeTo(targetFolder)
  }

  private fun Element.buildMetaProperty(
    modelElement: Element
  ): PropertySpec {
    val fieldType = modelElement.resolveFieldType(this)
    val comment = this.getAnnotationsByType(Comment::class.java)
      .map { it.comment }
      .firstOrNull()
      .orEmpty()
    val fieldName = fieldName()
    val nonNullFieldType = fieldType.copy(nullable = false)
    val rawFieldType = if (nonNullFieldType is ParameterizedTypeName) {
      nonNullFieldType.rawType.kotlin(false)
    } else {
      nonNullFieldType
    }
    val genericTypeArguments = if (nonNullFieldType is ParameterizedTypeName) {
      nonNullFieldType.typeArguments
    } else {
      emptyList()
    }
    val statmentArgs = mutableListOf(
      ModelMetaField::class.asClassName(),
      modelElement.typeName(),
      rawFieldType,
      fieldType.isNullable,
      comment,
    )
    for (genericTypeArgument in genericTypeArguments) {
      statmentArgs.add(genericTypeArgument)
    }
    return PropertySpec
      .builder(
        fieldName,
        ModelMetaField::class.asClassName().parameterizedBy(
          modelElement.typeName(),
          rawFieldType.generic(*genericTypeArguments.map { starType }.toTypedArray()),
        ),
        KModifier.PROTECTED
      )
      .initializer(
        CodeBlock.builder()
          .addStatement(
            """%T(
  modelClass=%T::class,
  fieldName="$fieldName",
  fieldClass=%T::class,
  fieldNullable=%L,
  comment=%S,
  genericClasses=arrayOf(${genericTypeArguments.joinToString(",") { "%T::class" }})
)""",
            *statmentArgs.toTypedArray()
          )
          .build()
      )
      .build()
  }

  private fun Element.buildMetaDslFn(
    modelElement: Element
  ): FunSpec {
    val fieldType = modelElement.resolveFieldType(this)
    val fieldName = fieldName()
    val nonNullFieldType = fieldType.copy(nullable = false)
    val rawFieldType = if (nonNullFieldType is ParameterizedTypeName) {
      nonNullFieldType.rawType.kotlin(false)
    } else {
      nonNullFieldType
    }
    val genericTypeArguments = if (nonNullFieldType is ParameterizedTypeName) {
      nonNullFieldType.typeArguments
    } else {
      emptyList()
    }
    return FunSpec.builder(fieldName)
      .addParameter(
        ParameterSpec(
          "dsl",
          LambdaTypeName.get(
            receiver = ModelMetaField::class.asClassName().parameterizedBy(
              modelElement.typeName(),
              rawFieldType.generic(*genericTypeArguments.map { starType }.toTypedArray()),
            ),
            returnType = Unit::class.asTypeName()
          )
        )
      )
      .addCode("dsl.invoke($fieldName)")
      .build()
  }
}
