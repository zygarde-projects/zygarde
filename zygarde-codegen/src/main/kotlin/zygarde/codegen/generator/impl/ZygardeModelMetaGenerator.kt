package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.ZygardeKaptOptions.Companion.MODEL_META_GENERATE_PACKAGE
import zygarde.codegen.extension.kotlinpoet.allSuperTypes
import zygarde.codegen.extension.kotlinpoet.fieldName
import zygarde.codegen.extension.kotlinpoet.nullableTypeName
import zygarde.codegen.extension.kotlinpoet.typeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.meta.ModelMetaField
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.jpa.entity.AutoLongIdEntity
import zygarde.data.jpa.entity.SequenceIntIdEntity
import zygarde.data.jpa.entity.SequenceLongIdEntity
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeModelMetaGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  fun generateModelMeta(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    elements.forEach {
      generateMetaFields(it)
    }
  }

  private fun generateMetaFields(entityElement: Element) {
    val className = entityElement.simpleName.toString()
    val pack = packageName(processingEnv.options.getOrDefault(MODEL_META_GENERATE_PACKAGE, "model.meta"))
    val fileNameForFields = "${className}Meta"
    val classBuilder = TypeSpec.objectBuilder(fileNameForFields)

    entityElement.allFieldsIncludeSuper().forEach { field ->
      classBuilder.addProperty(
        field.buildMetaProperty(entityElement)
      )
    }

    FileSpec.builder(pack, fileNameForFields)
      .addType(classBuilder.build())
      .build()
      .writeTo(folderToGenerate(ZygardeKaptOptions.MODEL_META_GENERATE_TARGET))
  }

  private fun Element.buildMetaProperty(
    entityElement: Element
  ): PropertySpec {
    val fieldType = entityElement.resolveFieldType(this)
    return PropertySpec
      .builder(
        fieldName(),
        ModelMetaField::class.asClassName().parameterizedBy(
          entityElement.typeName(),
          fieldType
        ),
        KModifier.PUBLIC
      )
      .initializer(
        CodeBlock.builder()
          .addStatement(
            """%T(%T::class,"${fieldName()}",%T::class,%L)""",
            ModelMetaField::class.asClassName(),
            entityElement.typeName(),
            fieldType,
            nullableTypeName().isNullable
          )
          .build()
      )
      .build()
  }

  private fun Element.resolveFieldType(fieldElement: Element): TypeName {
    val fieldTypeName = fieldElement.typeName()
    return if (fieldTypeName.toString() == "T") {
      val allSuperTypes = this.allSuperTypes(processingEnv)
      val isIdDefinedLong = allSuperTypes.any {
        it.typeName() == AutoLongIdEntity::class.asTypeName() || it.typeName() == SequenceLongIdEntity::class.asTypeName()
      }
      if (isIdDefinedLong) {
        Long::class.asTypeName()
      } else {
        val isIdDefinedInt = allSuperTypes.any {
          it.typeName() == AutoIntIdEntity::class.asTypeName() || it.typeName() == SequenceIntIdEntity::class.asTypeName()
        }
        if (isIdDefinedInt) {
          Int::class.asTypeName()
        } else {
          throw IllegalArgumentException("cannot resolve field type for $this $fieldTypeName")
        }
      }
    } else {
      fieldTypeName
    }
  }
}
