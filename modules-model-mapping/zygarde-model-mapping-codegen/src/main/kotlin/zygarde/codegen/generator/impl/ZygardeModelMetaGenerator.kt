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
import zygarde.codegen.dsl.ClassBasedModelMappingDslCodegen
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.core.annotation.Comment
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
    val modelFieldHolderClassName = "${className}ModelFields"
    val modelFieldHolderClassBuilder = TypeSpec.objectBuilder(modelFieldHolderClassName)

    val codegenClassName = "Abstract${className}Codegen"
    val codegenClassBuilder = TypeSpec.classBuilder(codegenClassName)
      .addModifiers(KModifier.ABSTRACT)
      .superclass(ClassBasedModelMappingDslCodegen::class.generic(modelElement.typeName()))
      .addSuperclassConstructorParameter("%T::class", modelElement.typeName())

    val allFieldsIncludeSuper = modelElement.allFieldsIncludeSuper()
    allFieldsIncludeSuper.forEach { field ->
      val metaProperty = field.buildMetaProperty(modelElement)
      codegenClassBuilder.addProperty(
        PropertySpec
          .builder(
            metaProperty.name,
            metaProperty.type,
            KModifier.PROTECTED,
          )
          .initializer("$modelFieldHolderClassName.${field.fieldName()}")
          .build()
      )
      codegenClassBuilder.addFunction(
        field.buildMetaDslFn(modelElement)
      )
      modelFieldHolderClassBuilder.addProperty(
        metaProperty
      )
    }

    val allFieldNames = allFieldsIncludeSuper.map { it.fieldName() }

    codegenClassBuilder.addProperty(
      PropertySpec
        .builder(
          "allFields",
          Array::class.generic(
            ModelMetaField::class.generic(
              modelElement.typeName(),
              starType
            )
          )
        )
        .initializer(
          "arrayOf(${allFieldNames.joinToString(",")})"
        )
        .build()
    )

    FileSpec.builder(pack, codegenClassName)
      .addType(codegenClassBuilder.build())
      .build()
      .writeTo(targetFolder)

    FileSpec.builder(pack, modelFieldHolderClassName)
      .addType(modelFieldHolderClassBuilder.build())
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
