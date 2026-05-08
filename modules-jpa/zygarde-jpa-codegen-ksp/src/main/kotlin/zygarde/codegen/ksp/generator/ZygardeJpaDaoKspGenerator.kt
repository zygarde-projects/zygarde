package zygarde.codegen.ksp.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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
import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortField
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.SearchSpecBuilder

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

    val generateRemove = daoInherit?.contains("ZygardeEnhancedDao") == true
    val toSpringDataSort = MemberName("zygarde.data.jpa.search.request", "toSpringDataSort")
    val toSpringDataPageRequest = MemberName("zygarde.data.jpa.search.request", "toSpringDataPageRequest")
    elements.forEach { element ->
      val entityName = element.simpleName.asString()
      val daoName = "$entityName$daoSuffix"
      val entityTypeName = element.asType(emptyList()).toTypeName().copy(nullable = false)
      val idTypeName = element.findIdClass()
      val daoType = ClassName(daoPackage, daoName)

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

      // Detect scope fields from @ScopeMarker interfaces
      val scopeFields = collectScopeFields(element)
      val scopeClassName = if (scopeFields.isNotEmpty()) {
        val scopeType = ClassName(daoPackage, "${entityName}Scope")
        generateScopeDataClass(daoPackage, entityName, scopeFields)
        scopeType
      } else {
        null
      }

      val searchContentType = searchContentLambdaType(entityTypeName)
      buildExtensionFileSpec(
        daoPackage,
        daoName,
        daoType,
        entityTypeName,
        searchContentType,
        toSpringDataSort,
        toSpringDataPageRequest,
        generateRemove,
        scopeClassName,
        scopeFields,
      ).build().writeTo(codeGenerator, aggregating = false)
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

  private data class ScopeFieldInfo(
    val name: String,
    val typeName: TypeName,
    val nullable: Boolean,
    val nullEquivalent: String?,
  )

  private fun collectScopeFields(element: KSClassDeclaration): List<ScopeFieldInfo> {
    val fields = mutableMapOf<String, ScopeFieldInfo>()
    collectScopeFieldsRecursive(element, fields)
    return fields.values.toList()
  }

  private fun collectScopeFieldsRecursive(
    declaration: KSClassDeclaration,
    result: MutableMap<String, ScopeFieldInfo>,
  ) {
    declaration.superTypes.forEach { superTypeRef ->
      val superType = superTypeRef.resolve()
      val superDeclaration = superType.declaration
      if (superDeclaration is KSClassDeclaration) {
        val scopeMarkerAnnotation = superDeclaration.annotations.find {
          it.shortName.asString() == "ScopeMarker"
        }
        if (scopeMarkerAnnotation != null) {
          superDeclaration.getAllProperties().forEach { prop ->
            val propName = prop.simpleName.asString()
            val propType = prop.type.resolve()
            val isNullable = propType.isMarkedNullable
            val nullEquivalentAnnotation = prop.annotations.find {
              it.shortName.asString() == "NullEquivalent"
            } ?: prop.getter?.annotations?.find {
              it.shortName.asString() == "NullEquivalent"
            }
            val nullEquivalent = nullEquivalentAnnotation
              ?.arguments?.find { it.name?.asString() == "value" }?.value as? String
            val typeName = propType.toTypeName().copy(nullable = false)
            result.putIfAbsent(
              propName,
              ScopeFieldInfo(
                name = propName,
                typeName = typeName,
                nullable = isNullable || nullEquivalent != null,
                nullEquivalent = nullEquivalent,
              )
            )
          }
        }
        // Recurse into super types
        collectScopeFieldsRecursive(superDeclaration, result)
      }
    }
  }

  private fun generateScopeDataClass(
    daoPackage: String,
    entityName: String,
    fields: List<ScopeFieldInfo>,
  ) {
    val scopeClassName = "${entityName}Scope"
    val collectionType = Collection::class.asClassName()

    val primaryConstructor = FunSpec.constructorBuilder()
    val properties = mutableListOf<PropertySpec>()

    fields.forEach { field ->
      val collectionOfField = collectionType.parameterizedBy(field.typeName)
      val paramType = if (field.nullable) collectionOfField.copy(nullable = true) else collectionOfField
      primaryConstructor.addParameter(
        ParameterSpec.builder(field.name, paramType)
          .also { if (field.nullable) it.defaultValue("null") }
          .build()
      )
      properties.add(
        PropertySpec.builder(field.name, paramType)
          .initializer(field.name)
          .build()
      )
    }

    val convenienceConstructor = FunSpec.constructorBuilder()
    val convenienceThisArgs = mutableListOf<CodeBlock>()
    fields.forEach { field ->
      val singleType = if (field.nullable) field.typeName.copy(nullable = true) else field.typeName
      convenienceConstructor.addParameter(
        ParameterSpec.builder(field.name, singleType)
          .also { if (field.nullable) it.defaultValue("null") }
          .build()
      )
      if (field.nullable) {
        convenienceThisArgs.add(CodeBlock.of("${field.name} = ${field.name}?.let·{ listOf(it) }"))
      } else {
        convenienceThisArgs.add(CodeBlock.of("${field.name} = listOf(${field.name})"))
      }
    }
    convenienceConstructor.callThisConstructor(convenienceThisArgs)

    val typeSpec = TypeSpec.classBuilder(scopeClassName)
      .addModifiers(KModifier.DATA)
      .primaryConstructor(primaryConstructor.build())
      .addProperties(properties)
      .addFunction(convenienceConstructor.build())
      .build()

    FileSpec.builder(daoPackage, scopeClassName)
      .addType(typeSpec)
      .build()
      .writeTo(codeGenerator, aggregating = false)
  }

  private fun buildExtensionFileSpec(
    daoPackage: String,
    daoName: String,
    daoType: ClassName,
    entityType: TypeName,
    searchContentType: LambdaTypeName,
    toSpringDataSort: MemberName,
    toSpringDataPageRequest: MemberName,
    generateRemove: Boolean,
    scopeClassName: ClassName?,
    scopeFields: List<ScopeFieldInfo>,
  ): FileSpec.Builder {
    val fileSpec = FileSpec.builder(daoPackage, "${daoName}Extensions")

    if (scopeClassName != null) {
      val scopeParam = ParameterSpec.builder("scope", scopeClassName).build()
      val defaultSearchContent = ParameterSpec.builder("searchContent", searchContentType)
        .defaultValue("{}")
        .build()

      fileSpec.addFunction(
        FunSpec.builder("search")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter(defaultSearchContent)
          .returns(List::class.asClassName().parameterizedBy(entityType))
          .addCode(buildScopedSearchBody(entityType, scopeFields, "findAll(%T.buildSpec(searchContent))", SearchSpecBuilder::class))
          .build()
      )

      fileSpec.addFunction(
        FunSpec.builder("search")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter(
            "sorts",
            List::class.asClassName().parameterizedBy(SortField::class.asClassName()).copy(nullable = true)
          )
          .addParameter("searchContent", searchContentType)
          .returns(List::class.asClassName().parameterizedBy(entityType))
          .addCode(
            buildScopedSearchBody(
              entityType,
              scopeFields,
              "sorts?.let·{ findAll(%T.buildSpec(searchContent), it.%M()) } ?: findAll(%T.buildSpec(searchContent))",
              SearchSpecBuilder::class,
              toSpringDataSort,
              SearchSpecBuilder::class,
            )
          )
          .build()
      )

      fileSpec.addFunction(
        FunSpec.builder("search")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter("searchContent", searchContentType)
          .addParameter("limit", Int::class)
          .returns(List::class.asClassName().parameterizedBy(entityType))
          .addCode(
            buildScopedSearchBody(
              entityType,
              scopeFields,
              "findAll(%T.buildSpec(searchContent), %T.of(0, limit)).content",
              SearchSpecBuilder::class,
              PageRequest::class
            )
          )
          .build()
      )

      fileSpec.addFunction(
        FunSpec.builder("searchCount")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter(defaultSearchContent)
          .returns(Long::class)
          .addCode(buildScopedSearchBody(entityType, scopeFields, "count(%T.buildSpec(searchContent))", SearchSpecBuilder::class))
          .build()
      )

      fileSpec.addFunction(
        FunSpec.builder("searchOne")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter(defaultSearchContent)
          .returns(entityType.copy(nullable = true))
          .addCode(
            buildScopedSearchBody(
              entityType,
              scopeFields,
              "findOne(%T.buildSpec(searchContent)).let·{ if (it.isPresent) it.get() else null }",
              SearchSpecBuilder::class,
            )
          )
          .build()
      )

      fileSpec.addFunction(
        FunSpec.builder("searchOneOrThrow")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter("errorCode", ErrorCode::class)
          .addParameter(defaultSearchContent)
          .returns(entityType)
          .addStatement(
            "return searchOne(scope, searchContent) ?: throw %T(errorCode)",
            BusinessException::class,
          )
          .build()
      )

      fileSpec.addFunction(
        FunSpec.builder("searchPage")
          .receiver(daoType)
          .addParameter(scopeParam)
          .addParameter("req", PagingAndSortingRequest::class)
          .addParameter(defaultSearchContent)
          .returns(Page::class.asClassName().parameterizedBy(entityType))
          .addCode(
            buildScopedSearchBody(
              entityType,
              scopeFields,
              "findAll(%T.buildSpec(searchContent), req.%M())",
              SearchSpecBuilder::class,
              toSpringDataPageRequest,
            )
          )
          .build()
      )

      if (generateRemove) {
        fileSpec.addFunction(
          FunSpec.builder("remove")
            .receiver(daoType)
            .addParameter(scopeParam)
            .addParameter(defaultSearchContent)
            .returns(Long::class)
            .addCode(buildScopedSearchBody(entityType, scopeFields, "delete(%T.buildSpec(searchContent))", SearchSpecBuilder::class))
            .build()
        )
      }
    } else {
      fileSpec.addFunction(
        FunSpec.builder("search")
          .receiver(daoType)
          .addParameter("searchContent", searchContentType)
          .returns(List::class.asClassName().parameterizedBy(entityType))
          .addStatement("return findAll(%T.buildSpec(searchContent))", SearchSpecBuilder::class)
          .build()
      )
        .addFunction(
          FunSpec.builder("search")
            .receiver(daoType)
            .addParameter(
              "sorts",
              List::class.asClassName().parameterizedBy(SortField::class.asClassName()).copy(nullable = true)
            )
            .addParameter("searchContent", searchContentType)
            .returns(List::class.asClassName().parameterizedBy(entityType))
            .addStatement(
              "return sorts?.let·{ findAll(%T.buildSpec(searchContent), it.%M()) } ?: search(searchContent)",
              SearchSpecBuilder::class,
              toSpringDataSort,
            )
            .build()
        )
        .addFunction(
          FunSpec.builder("search")
            .receiver(daoType)
            .addParameter("searchContent", searchContentType)
            .addParameter("limit", Int::class)
            .returns(List::class.asClassName().parameterizedBy(entityType))
            .addStatement(
              "return findAll(%T.buildSpec(searchContent), %T.of(0, limit)).content",
              SearchSpecBuilder::class,
              PageRequest::class,
            )
            .build()
        )
        .addFunction(
          FunSpec.builder("searchCount")
            .receiver(daoType)
            .addParameter("searchContent", searchContentType)
            .returns(Long::class)
            .addStatement("return count(%T.buildSpec(searchContent))", SearchSpecBuilder::class)
            .build()
        )
        .addFunction(
          FunSpec.builder("searchOne")
            .receiver(daoType)
            .addParameter("searchContent", searchContentType)
            .returns(entityType.copy(nullable = true))
            .addStatement(
              "return findOne(%T.buildSpec(searchContent)).let·{ if (it.isPresent) it.get() else null }",
              SearchSpecBuilder::class,
            )
            .build()
        )
        .addFunction(
          FunSpec.builder("searchOneOrThrow")
            .receiver(daoType)
            .addParameter("errorCode", ErrorCode::class)
            .addParameter("searchContent", searchContentType)
            .returns(entityType)
            .addStatement(
              "return searchOne(searchContent) ?: throw %T(errorCode)",
              BusinessException::class,
            )
            .build()
        )
        .addFunction(
          FunSpec.builder("searchPage")
            .receiver(daoType)
            .addParameter("req", PagingAndSortingRequest::class)
            .addParameter("searchContent", searchContentType)
            .returns(Page::class.asClassName().parameterizedBy(entityType))
            .addStatement(
              "return findAll(%T.buildSpec(searchContent), req.%M())",
              SearchSpecBuilder::class,
              toSpringDataPageRequest,
            )
            .build()
        )

      if (generateRemove) {
        fileSpec.addFunction(
          FunSpec.builder("remove")
            .receiver(daoType)
            .addParameter("searchContent", searchContentType)
            .returns(Long::class)
            .addStatement("return delete(%T.buildSpec(searchContent))", SearchSpecBuilder::class)
            .build()
        )
      }
    }

    return fileSpec
  }

  private fun buildScopedSearchBody(
    entityType: TypeName,
    scopeFields: List<ScopeFieldInfo>,
    returnStatement: String,
    vararg returnArgs: Any,
  ): CodeBlock {
    val builder = CodeBlock.builder()
    builder.addStatement("val originalSearchContent = searchContent")
    builder.beginControlFlow(
      "val searchContent: %T.() -> %T = ",
      EnhancedSearch::class.asClassName().parameterizedBy(entityType),
      UNIT,
    )

    scopeFields.forEach { field ->
      if (field.nullEquivalent != null) {
        if (field.nullable) {
          builder.beginControlFlow("scope.${field.name}?.let·{ scopeValues ->")
          builder.beginControlFlow("if (scopeValues.any { it.toString() == %S })", field.nullEquivalent)
          builder.beginControlFlow("or")
          builder.addStatement("field<%T>(%S) inList scopeValues", field.typeName, field.name)
          builder.addStatement("field<%T>(%S).isNull()", field.typeName, field.name)
          builder.endControlFlow()
          builder.nextControlFlow("else")
          builder.addStatement("field<%T>(%S) inList scopeValues", field.typeName, field.name)
          builder.endControlFlow()
          builder.endControlFlow()
        } else {
          builder.beginControlFlow("scope.${field.name}.let·{ scopeValues ->")
          builder.beginControlFlow("if (scopeValues.any { it.toString() == %S })", field.nullEquivalent)
          builder.beginControlFlow("or")
          builder.addStatement("field<%T>(%S) inList scopeValues", field.typeName, field.name)
          builder.addStatement("field<%T>(%S).isNull()", field.typeName, field.name)
          builder.endControlFlow()
          builder.nextControlFlow("else")
          builder.addStatement("field<%T>(%S) inList scopeValues", field.typeName, field.name)
          builder.endControlFlow()
          builder.endControlFlow()
        }
      } else if (field.nullable) {
        builder.addStatement("scope.${field.name}?.let·{ field<%T>(%S) inList it }", field.typeName, field.name)
      } else {
        builder.addStatement("field<%T>(%S) inList scope.${field.name}", field.typeName, field.name)
      }
    }

    builder.addStatement("originalSearchContent()")
    builder.endControlFlow()
    builder.addStatement("return $returnStatement", *returnArgs)
    return builder.build()
  }

  private fun searchContentLambdaType(entityType: TypeName): LambdaTypeName {
    return LambdaTypeName.get(
      receiver = EnhancedSearch::class.asClassName().parameterizedBy(entityType),
      returnType = UNIT,
    )
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
