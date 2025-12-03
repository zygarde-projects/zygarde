package zygarde.codegen.ksp.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Component
import zygarde.codegen.ksp.ZygardeJpaKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_COMBINE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_INHERIT
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_PACKAGE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_SUFFIX
import zygarde.codegen.ksp.extension.generic
import zygarde.codegen.ksp.extension.kotlin

class ZygardeJpaDaoKspGenerator(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String>
) {
  private val daoInherit: String? by lazy {
    options[DAO_INHERIT]
  }

  private fun packageName(pack: String): String {
    val basePackage = options.getOrDefault(BASE_PACKAGE, "zygarde.generated")
    return "$basePackage.$pack"
  }

  fun generateDaoForEntityElements(elements: Collection<KSClassDeclaration>) {
    if (elements.isEmpty()) {
      return
    }
    val daoPackage = packageName(options.getOrDefault(DAO_PACKAGE, "data.dao"))
    val daoSuffix = options.getOrDefault(DAO_SUFFIX, "Dao")

    elements.forEach { element ->
      val entityName = element.simpleName.asString()
      val daoName = "$entityName$daoSuffix"
      val entityTypeName = element.asType(emptyList()).toTypeName().copy(nullable = false)
      val idTypeName = element.findIdClass()

      FileSpec.builder(daoPackage, daoName)
        .addType(
          TypeSpec.interfaceBuilder(daoName)
            .also { interfaceBuilder ->
              val superInterface = daoInherit?.let { ClassName.bestGuess(it) }
              if (superInterface != null) {
                interfaceBuilder.addSuperinterface(
                  superInterface.generic(entityTypeName, idTypeName)
                )
              } else {
                interfaceBuilder.addSuperinterface(
                  JpaRepository::class.generic(entityTypeName, idTypeName)
                )
                  .addSuperinterface(
                    JpaSpecificationExecutor::class.generic(entityTypeName)
                  )
              }
            }
            .build()
        )
        .build()
        .writeTo(codeGenerator, aggregating = false)
    }

    if (options.getOrDefault(DAO_COMBINE, "true") == "true") {
      val classBuilder = TypeSpec.classBuilder("Dao").addAnnotation(Component::class)
      val constructorBuilder = FunSpec.constructorBuilder()

      elements.sortedBy { it.asType(emptyList()).toTypeName().toString() }.forEach {
        val entityName = it.simpleName.asString()
        val entityFieldName = entityName.replaceFirstChar { c -> c.lowercase() }
        val daoFieldName = "$entityFieldName$daoSuffix"
        val daoClass = ClassName(daoPackage, "$entityName$daoSuffix")
        classBuilder.addProperty(
          PropertySpec
            .builder(daoFieldName, daoClass)
            .initializer(daoFieldName)
            .addAnnotation(Autowired::class)
            .build()
        )
        constructorBuilder.addParameter(
          ParameterSpec
            .builder(daoFieldName, daoClass)
            .build()
        )
      }

      FileSpec.builder(daoPackage, "Dao")
        .addType(
          classBuilder
            .primaryConstructor(constructorBuilder.build())
            .build()
        )
        .build()
        .writeTo(codeGenerator, aggregating = false)
    }
  }

  @OptIn(KspExperimental::class)
  private fun KSClassDeclaration.findIdClass(): TypeName {
    // Check for @IdClass annotation
    val idClassAnnotation = annotations.find {
      it.shortName.asString() == "IdClass"
    }
    if (idClassAnnotation != null) {
      val idClassArg = idClassAnnotation.arguments.find { it.name?.asString() == "value" }
      val idClassType = idClassArg?.value as? KSType
      if (idClassType != null) {
        return idClassType.toTypeName().kotlin(canBeNullable = false)
      }
    }

    // Find property annotated with @Id
    val allProperties = getAllPropertiesIncludingSuper()
    val idProperty = allProperties.find { prop ->
      prop.annotations.any { it.shortName.asString() == "Id" }
    }

    if (idProperty != null) {
      return idProperty.type.resolve().toTypeName().kotlin(canBeNullable = false)
    }

    throw IllegalStateException("No @Id or @IdClass found for entity ${simpleName.asString()}")
  }

  private fun KSClassDeclaration.getAllPropertiesIncludingSuper(): List<KSPropertyDeclaration> {
    val props = mutableListOf<KSPropertyDeclaration>()
    props.addAll(getAllProperties().toList())

    superTypes.forEach { superTypeRef ->
      val superType = superTypeRef.resolve()
      val superDeclaration = superType.declaration
      if (superDeclaration is KSClassDeclaration) {
        val superTypeName = superDeclaration.qualifiedName?.asString()
        if (superTypeName != null &&
          !superTypeName.startsWith("kotlin.") &&
          !superTypeName.startsWith("java.")
        ) {
          props.addAll(superDeclaration.getAllPropertiesIncludingSuper())
        }
      }
    }

    return props.distinctBy { it.simpleName.asString() }
  }
}
