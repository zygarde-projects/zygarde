package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import zygarde.codegen.ZygardeKaptOptions.Companion.ENTITY_PACKAGE_SEARCH
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allFieldsIncludeSuper
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.Searchable
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.action.impl.SearchableImpl
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Transient

class ZygardeEntityFieldGenerator(
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
      try {
        generateFields(it)
        generateExtensionFunctions(elements, it)
      } catch (e: IllegalArgumentException) {
        throw e
      } catch (t: Throwable) {
        throw RuntimeException("error generating search field for entity $it", t)
      }
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
      .writeTo(folderToGenerate())
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
          .addFunction(
            FunSpec.builder(fieldName)
              .addTypeVariable(TypeVariableName("T"))
              .receiver(
                ConditionAction::class.asClassName().parameterizedBy(
                  TypeVariableName("T"),
                  TypeVariableName("*"),
                  rootEntityType,
                )
              )
              .returns(
                field.toConditionAction(
                  TypeVariableName("T"),
                  TypeVariableName("*"),
                  element.resolveFieldType(field)
                )
              )
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

    fileBuilderForExtension.build().writeTo(folderToGenerate())
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
          entityElement.typeName(),
          entityElement.resolveFieldType(this).copy(nullable = false)
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
    return this.toConditionAction(rootEntityTypeName, currentEntityTypeName, currentEntityElement.resolveFieldType(this))
  }

  private fun Element.toConditionAction(rootEntityTypeName: TypeName, currentEntityTypeName: TypeName, fieldType: TypeName): TypeName {
    val nonNullableFieldType = fieldType.copy(nullable = false)
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
          nonNullableFieldType,
        )
      }
    } else {
      ConditionAction::class.asClassName().parameterizedBy(
        rootEntityTypeName,
        currentEntityTypeName,
        nonNullableFieldType,
      )
    }
  }
}
