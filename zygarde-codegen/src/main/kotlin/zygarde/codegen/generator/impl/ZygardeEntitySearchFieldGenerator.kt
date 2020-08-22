package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import zygarde.codegen.ZygardeKaptOptions.Companion.ENTITY_PACKAGE_SEARCH
import zygarde.codegen.extension.kotlinpoet.*
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.jpa.entity.AutoLongIdEntity
import zygarde.data.jpa.entity.SequenceIntIdEntity
import zygarde.data.jpa.entity.SequenceLongIdEntity
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.Searchable
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.action.impl.SearchableImpl
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.persistence.*

class ZygardeEntitySearchFieldGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  val erasuredComparable: TypeMirror by lazy {
    val comparableType = processingEnv.elementUtils.getTypeElement("java.lang.Comparable").asType()
    processingEnv.typeUtils.erasure(comparableType)
  }

  fun generateSearchFieldForEntityElements(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    elements.forEach {
      generateFields(it)
      generateExtensionFunctions(elements, it)
    }
  }

  private fun generateFields(element: Element) {
    val className = element.simpleName.toString()
    val pack = packageName(processingEnv.options.getOrDefault(ENTITY_PACKAGE_SEARCH, "entity.search"))
    val fileNameForFields = "${className}Fields"

    val classBuilder = TypeSpec.objectBuilder(fileNameForFields)
    element.allSearchableFields().forEach { field ->
      classBuilder.addProperty(
        field.buildSearchableProperty(element)
      )
    }

    FileSpec.builder(pack, fileNameForFields)
      .addType(classBuilder.build())
      .build()
      .writeTo(fileTarget)
  }

  private fun generateExtensionFunctions(allEntityElements: Collection<Element>, element: Element) {
    val className = element.simpleName.toString()
    val pack = packageName(processingEnv.options.getOrDefault(ENTITY_PACKAGE_SEARCH, "entity.search"))
    val fileNameForExtension = "${className}Extensions"
    val fileBuilderForExtension = FileSpec.builder(pack, fileNameForExtension)
    val rootEntityType = element.typeName()

    val allFields = element.allSearchableFields()
    allFields
      .forEach { field ->
        val fieldName = field.simpleName.toString()

        fileBuilderForExtension
          .addFunction(
            FunSpec.builder(fieldName)
              .receiver(EnhancedSearch::class.asClassName().parameterizedBy(rootEntityType))
              .returns(field.toConditionAction(element, element))
              .addStatement("return this.field(${className}Fields.$fieldName)")
              .build()
          )
      }

    allEntityElements
      .filter {
        allFields.any { f -> f.asType() == it.asType() }
      }
      .forEach { relatedTypeElement ->
        relatedTypeElement.allSearchableFields().forEach { fieldForRelativeType ->
          fileBuilderForExtension
            .addFunction(
              fieldForRelativeType.buildRelateTypeConditionAction(element, relatedTypeElement)
            )
        }
      }

    fileBuilderForExtension.build().writeTo(fileTarget)
  }

  private fun Element.allSearchableFields(): List<Element> {
    return allFieldsIncludeSuper(processingEnv)
      .filter {
        it.getAnnotation(Transient::class.java) == null &&
          it.getAnnotation(OneToMany::class.java) == null &&
          it.getAnnotation(ManyToMany::class.java) == null
      }
  }

  private fun Element.buildSearchableProperty(
    entityElement: Element
  ): PropertySpec {
    return PropertySpec
      .builder(
        fieldName(),
        Searchable::class.asClassName().parameterizedBy(
          entityElement.asType().asTypeName(),
          entityElement.resolveFieldType(this)
        ),
        KModifier.PUBLIC
      )
      .initializer(
        CodeBlock.builder()
          .addStatement("""%T("${fieldName()}")""", SearchableImpl::class.asClassName())
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

  private fun Element.buildRelateTypeConditionAction(rootEntityElement: Element, currentEntityElement: Element): FunSpec {
    return FunSpec.builder(fieldName())
      .addAnnotation(
        AnnotationSpec.builder(JvmName::class)
          .addMember("%S", "${currentEntityElement.fieldName()}_${fieldName()}")
          .build()
      )
      .receiver(
        ConditionAction::class.asClassName().parameterizedBy(
          rootEntityElement.typeName(),
          rootEntityElement.typeName(),
          currentEntityElement.typeName()
        )
      )
      .returns(
        toConditionAction(rootEntityElement, currentEntityElement)
      )
      .addStatement("return this.field(${currentEntityElement.simpleName}Fields.${fieldName()})")
      .build()
  }

  private fun Element.isComparable(): Boolean {
    return processingEnv.typeUtils.isAssignable(this.asType(), erasuredComparable)
  }

  private fun Element.toConditionAction(rootEntityElement: Element, currentEntityElement: Element): TypeName {
    val rootEntityTypeName = rootEntityElement.typeName()
    val currentEntityTypeName = currentEntityElement.typeName()
    return if (isComparable()) {
      if (this.typeName().toString() == "kotlin.String") {
        StringConditionAction::class.asClassName().parameterizedBy(
          rootEntityTypeName,
          currentEntityTypeName
        )
      } else {
        ComparableConditionAction::class.asClassName().parameterizedBy(
          rootEntityTypeName,
          currentEntityTypeName,
          currentEntityElement.resolveFieldType(this)
        )
      }
    } else {
      ConditionAction::class.asClassName().parameterizedBy(
        rootEntityTypeName,
        currentEntityTypeName,
        currentEntityElement.resolveFieldType(this)
      )
    }
  }
}
