package zygarde.codegen.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import zygarde.codegen.ksp.ZygardeJpaKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.ENTITY_PACKAGE_SEARCH
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction

class ZygardeEntityFieldKspGenerator(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String>
) {
  private fun packageName(pack: String): String {
    val basePackage = options.getOrDefault(BASE_PACKAGE, "zygarde.generated")
    return "$basePackage.$pack"
  }

  fun generateSearchFieldForEntityElements(elements: Collection<KSClassDeclaration>) {
    if (elements.isEmpty()) {
      return
    }
    elements.forEach { element ->
      try {
        generateExtensionFunctions(element)
      } catch (e: IllegalArgumentException) {
        throw e
      } catch (t: Throwable) {
        throw RuntimeException("error generating search field for entity ${element.simpleName.asString()}", t)
      }
    }
  }

  private fun generateExtensionFunctions(element: KSClassDeclaration) {
    val className = element.simpleName.asString()
    val pack = packageName(options.getOrDefault(ENTITY_PACKAGE_SEARCH, "entity.search"))
    val fileNameForExtension = "${className}Extensions"
    val fileBuilderForExtension = FileSpec.builder(pack, fileNameForExtension)
    val rootEntityType = element.asType(emptyList()).toTypeName().copy(nullable = false)

    val allFields = element.allSearchableFields()
    allFields.forEach { field ->
      val fieldName = field.simpleName.asString()
      val fieldConditionFunction = "field"
      fileBuilderForExtension
        .addFunction(
          FunSpec.builder(fieldName)
            .receiver(EnhancedSearch::class.asClassName().parameterizedBy(rootEntityType))
            .returns(field.toConditionAction(rootEntityType, rootEntityType))
            .addStatement("return this.$fieldConditionFunction($className::$fieldName)")
            .build()
        )
        .addFunction(
          FunSpec.builder(fieldName)
            .addTypeVariable(TypeVariableName("T"))
            .receiver(
              ConditionAction::class.asClassName().parameterizedBy(
                TypeVariableName("T"),
                STAR,
                rootEntityType,
              )
            )
            .returns(
              field.toConditionAction(
                TypeVariableName("T"),
                STAR,
                field.type.resolve().toTypeName().copy(nullable = false)
              )
            )
            .addStatement("return this.$fieldConditionFunction($className::$fieldName)")
            .build()
        )
    }

    fileBuilderForExtension.build().writeTo(codeGenerator, aggregating = false)
  }

  private fun KSClassDeclaration.allSearchableFields(): List<KSPropertyDeclaration> {
    return getAllProperties()
      .filter { prop ->
        val annotations = prop.annotations.map { it.shortName.asString() }.toSet()
        !annotations.contains("ElementCollection") &&
          !annotations.contains("Transient") &&
          !annotations.contains("OneToMany") &&
          !annotations.contains("ManyToMany") &&
          !annotations.contains("Convert")
      }
      .toList()
  }

  private fun KSPropertyDeclaration.isString(): Boolean {
    val typeName = type.resolve().declaration.qualifiedName?.asString()
    return typeName == "kotlin.String" || typeName == "java.lang.String"
  }

  private fun KSPropertyDeclaration.isComparable(): Boolean {
    val resolved = type.resolve()
    return isComparableType(resolved)
  }

  private fun isComparableType(type: KSType): Boolean {
    val qualifiedName = type.declaration.qualifiedName?.asString() ?: return false

    // Common Comparable types
    val comparableTypes = setOf(
      "kotlin.Int", "kotlin.Long", "kotlin.Short", "kotlin.Byte",
      "kotlin.Float", "kotlin.Double", "kotlin.Char",
      "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
      "java.lang.Float", "java.lang.Double", "java.lang.Character",
      "java.lang.String", "kotlin.String",
      "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
      "java.time.Instant", "java.time.ZonedDateTime", "java.time.OffsetDateTime",
      "java.util.Date", "java.math.BigDecimal", "java.math.BigInteger"
    )

    if (qualifiedName in comparableTypes) {
      return true
    }

    // Check if implements Comparable interface
    val declaration = type.declaration
    if (declaration is KSClassDeclaration) {
      return declaration.superTypes.any { superType ->
        val superTypeName = superType.resolve().declaration.qualifiedName?.asString()
        superTypeName == "java.lang.Comparable" || superTypeName == "kotlin.Comparable"
      }
    }

    return false
  }

  private fun KSPropertyDeclaration.toConditionAction(
    rootEntityTypeName: TypeName,
    currentEntityTypeName: TypeName
  ): TypeName {
    val fieldType = type.resolve().toTypeName().copy(nullable = false)
    return toConditionAction(rootEntityTypeName, currentEntityTypeName, fieldType)
  }

  private fun KSPropertyDeclaration.toConditionAction(
    rootEntityTypeName: TypeName,
    currentEntityTypeName: TypeName,
    fieldType: TypeName
  ): TypeName {
    val nonNullableFieldType = fieldType.copy(nullable = false)
    return when {
      isString() -> {
        StringConditionAction::class.asClassName().parameterizedBy(
          rootEntityTypeName,
          currentEntityTypeName
        )
      }

      isComparable() -> {
        ComparableConditionAction::class.asClassName().parameterizedBy(
          rootEntityTypeName,
          currentEntityTypeName,
          nonNullableFieldType,
        )
      }

      else -> {
        ConditionAction::class.asClassName().parameterizedBy(
          rootEntityTypeName,
          currentEntityTypeName,
          nonNullableFieldType,
        )
      }
    }
  }
}
