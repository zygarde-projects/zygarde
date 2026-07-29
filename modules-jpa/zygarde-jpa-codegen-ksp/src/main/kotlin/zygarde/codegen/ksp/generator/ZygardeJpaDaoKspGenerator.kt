package zygarde.codegen.ksp.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Component
import zygarde.codegen.ScopeOperator
import zygarde.codegen.ksp.ZygardeJpaKspOptions.BASE_PACKAGE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_COMBINE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_INHERIT
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_PACKAGE
import zygarde.codegen.ksp.ZygardeJpaKspOptions.DAO_SUFFIX
import zygarde.codegen.ksp.extension.generic
import zygarde.codegen.ksp.extension.kotlin
import zygarde.core.exception.BusinessException
import zygarde.core.exception.CommonErrorCode
import zygarde.core.exception.ErrorCode
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortField
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.SearchSpecBuilder
import zygarde.data.jpa.search.ScopeFilter

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
    val operator: ScopeOperator,
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
            val nullEquivalentAnnotation = prop.getter?.annotations?.find {
              it.shortName.asString() == "NullEquivalent"
            }
            val nullEquivalent = nullEquivalentAnnotation
              ?.arguments?.find { it.name?.asString() == "value" }?.value as? String
            val scopeOpAnnotation = prop.getter?.annotations?.find {
              it.shortName.asString() == "ScopeOp"
            }
            val operator = scopeOpAnnotation
              ?.arguments?.find { it.name?.asString() == "value" }
              ?.value?.toString()?.substringAfterLast('.')?.let(ScopeOperator::valueOf)
              ?: ScopeOperator.IN
            val isMembership = operator == ScopeOperator.IN || operator == ScopeOperator.NOT_IN
            val typeName = propType.toTypeName().copy(nullable = false)
            result.putIfAbsent(
              propName,
              ScopeFieldInfo(
                name = propName,
                typeName = typeName,
                nullable = isNullable || (isMembership && nullEquivalent != null),
                nullEquivalent = nullEquivalent.takeIf { isMembership },
                operator = operator,
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
    val javaCollectionType = ClassName("java.util", "Collection")
    val scopeFilterType = ScopeFilter::class.asClassName()

    val primaryConstructor = FunSpec.constructorBuilder()
    val properties = mutableListOf<PropertySpec>()

    fields.forEach { field ->
      val fieldScopeType = when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> scopeFilterType.parameterizedBy(field.typeName)
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> Boolean::class.asTypeName()
      }
      val paramType = when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> fieldScopeType
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> if (field.nullable) fieldScopeType.copy(nullable = true) else fieldScopeType
      }
      primaryConstructor.addParameter(
        ParameterSpec.builder(field.name, paramType)
          .also {
            if (field.nullable) {
              if (field.operator == ScopeOperator.IN || field.operator == ScopeOperator.NOT_IN) {
                it.defaultValue("%T.All", ScopeFilter::class)
              } else {
                it.defaultValue("null")
              }
            }
          }
          .build()
      )
      properties.add(
        PropertySpec.builder(field.name, paramType)
          .initializer(field.name)
          .build()
      )
    }

    val collectionConstructor = FunSpec.constructorBuilder()
    val collectionThisArgs = mutableListOf<CodeBlock>()
    val requiredCompatibilityField = fields
      .filter { it.operator == ScopeOperator.IN || it.operator == ScopeOperator.NOT_IN }
      .takeIf { membershipFields -> membershipFields.all { it.nullable } }
      ?.firstOrNull()
    fields.forEach { field ->
      val baseType = when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> collectionType.parameterizedBy(field.typeName)
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> Boolean::class.asTypeName()
      }
      val paramType = if (field.nullable) baseType.copy(nullable = true) else baseType
      collectionConstructor.addParameter(
        ParameterSpec.builder(field.name, paramType)
          .also { if (field.nullable && field != requiredCompatibilityField) it.defaultValue("null") }
          .build()
      )
      when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> if (field.nullable) {
          collectionThisArgs.add(
            CodeBlock.of("${field.name} = ${field.name}?.let·{ %T.Of(it) } ?: %T.All", ScopeFilter::class, ScopeFilter::class)
          )
        } else {
          collectionThisArgs.add(CodeBlock.of("${field.name} = %T.Of(${field.name})", ScopeFilter::class))
        }
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> {
          collectionThisArgs.add(CodeBlock.of("${field.name} = ${field.name}"))
        }
      }
    }
    collectionConstructor.callThisConstructor(collectionThisArgs)

    val convenienceConstructor = FunSpec.constructorBuilder()
    val convenienceThisArgs = mutableListOf<CodeBlock>()
    fields.forEach { field ->
      val baseType = when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> field.typeName
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> Boolean::class.asTypeName()
      }
      val singleType = if (field.nullable && field != requiredCompatibilityField) baseType.copy(nullable = true) else baseType
      convenienceConstructor.addParameter(
        ParameterSpec.builder(field.name, singleType)
          .also { if (field.nullable && field != requiredCompatibilityField) it.defaultValue("null") }
          .build()
      )
      when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> if (field.nullable && field != requiredCompatibilityField) {
          convenienceThisArgs.add(
            CodeBlock.of("${field.name} = ${field.name}?.let·{ %T.Of(listOf(it)) } ?: %T.All", ScopeFilter::class, ScopeFilter::class)
          )
        } else {
          convenienceThisArgs.add(CodeBlock.of("${field.name} = %T.Of(listOf(${field.name}))", ScopeFilter::class))
        }
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> {
          convenienceThisArgs.add(CodeBlock.of("${field.name} = ${field.name}"))
        }
      }
    }
    convenienceConstructor.callThisConstructor(convenienceThisArgs)

    val hasMembershipField = fields.any { it.operator == ScopeOperator.IN || it.operator == ScopeOperator.NOT_IN }
    val convenienceErasedTypes = fields.map { field ->
      when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> when (val type = field.typeName) {
          is ClassName -> type
          is ParameterizedTypeName -> type.rawType
          else -> null
        }
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> Boolean::class.asClassName()
      }
    }
    val convenienceCollidesWithPrimary = fields.zip(convenienceErasedTypes).all { (field, erasedType) ->
      field.operator == ScopeOperator.IS_NULL ||
        field.operator == ScopeOperator.IS_NOT_NULL ||
        erasedType == scopeFilterType
    }
    val convenienceCollidesWithCollection = fields.zip(convenienceErasedTypes).all { (field, erasedType) ->
      field.operator == ScopeOperator.IS_NULL ||
        field.operator == ScopeOperator.IS_NOT_NULL ||
        erasedType == collectionType ||
        erasedType == javaCollectionType
    }
    val typeSpec = TypeSpec.classBuilder(scopeClassName)
      .addModifiers(KModifier.DATA)
      .primaryConstructor(primaryConstructor.build())
      .addProperties(properties)
      .also {
        if (hasMembershipField) {
          it.addFunction(collectionConstructor.build())
          if (!convenienceCollidesWithPrimary && !convenienceCollidesWithCollection) {
            it.addFunction(convenienceConstructor.build())
          }
        }
      }
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
        buildPatchOneFunction(
          daoType = daoType,
          entityType = entityType,
          searchContentType = searchContentType,
          scopeClassName = scopeClassName,
        )
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
      addScopeRequiredOverloads(
        fileSpec,
        daoType,
        entityType,
        searchContentType,
        generateRemove,
        scopeClassName,
        scopeFields,
      )
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
          buildPatchOneFunction(
            daoType = daoType,
            entityType = entityType,
            searchContentType = searchContentType,
            scopeClassName = null,
          )
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

  private fun buildPatchOneFunction(
    daoType: ClassName,
    entityType: TypeName,
    searchContentType: LambdaTypeName,
    scopeClassName: ClassName?,
  ): FunSpec {
    val patchType = TypeVariableName("PATCH")
    val patchContentType = LambdaTypeName.get(
      receiver = entityType,
      parameters = listOf(ParameterSpec.builder("patch", patchType).build()),
      returnType = UNIT,
    )
    return FunSpec.builder("patchOne")
      .receiver(daoType)
      .addTypeVariable(patchType)
      .also { builder ->
        scopeClassName?.let { builder.addParameter("scope", it) }
      }
      .addParameter("patch", patchType)
      .addParameter(
        ParameterSpec.builder("errorCode", ErrorCode::class)
          .defaultValue("%T.ERROR", CommonErrorCode::class)
          .build()
      )
      .addParameter("patchContent", patchContentType)
      .addParameter("searchContent", searchContentType)
      .returns(entityType)
      .addCode(buildPatchOneBody(scopeClassName != null))
      .build()
  }

  private fun buildPatchOneBody(
    scoped: Boolean,
  ): CodeBlock {
    val builder = CodeBlock.builder()
    if (scoped) {
      builder.addStatement("val entity = searchOneOrThrow(scope, errorCode, searchContent)")
    } else {
      builder.addStatement("val entity = searchOneOrThrow(errorCode, searchContent)")
    }
    builder.addStatement("entity.patchContent(patch)")
    builder.addStatement("return save(entity)")
    return builder.build()
  }

  private fun addScopeRequiredOverloads(
    fileSpec: FileSpec.Builder,
    daoType: ClassName,
    entityType: TypeName,
    searchContentType: LambdaTypeName,
    generateRemove: Boolean,
    scopeClassName: ClassName,
    scopeFields: List<ScopeFieldInfo>,
  ) {
    val scopeExample = buildScopeExample(scopeClassName, scopeFields)
    val message = "此實體已啟用 ScopedQueries：請以第一參數傳入 ${scopeClassName.simpleName}" +
      "（全放行請顯式傳 $scopeExample）"
    val deprecated = AnnotationSpec.builder(Deprecated::class)
      .addMember("%S", message)
      .addMember("level = %T.ERROR", DeprecationLevel::class)
      .build()
    val defaultSearchContent = ParameterSpec.builder("searchContent", searchContentType)
      .defaultValue("{}")
      .build()

    fun placeholder(name: String, returnType: TypeName, vararg parameters: ParameterSpec): FunSpec {
      return FunSpec.builder(name)
        .receiver(daoType)
        .addAnnotation(deprecated)
        .addParameters(parameters.toList())
        .returns(returnType)
        .addStatement("error(%S)", message)
        .build()
    }

    fileSpec.addFunction(
      placeholder(
        "search",
        List::class.asClassName().parameterizedBy(entityType),
        defaultSearchContent,
      )
    )
    fileSpec.addFunction(
      placeholder(
        "search",
        List::class.asClassName().parameterizedBy(entityType),
        ParameterSpec.builder(
          "sorts",
          List::class.asClassName().parameterizedBy(SortField::class.asClassName()).copy(nullable = true),
        ).build(),
        ParameterSpec.builder("searchContent", searchContentType).build(),
      )
    )
    fileSpec.addFunction(
      placeholder(
        "search",
        List::class.asClassName().parameterizedBy(entityType),
        ParameterSpec.builder("searchContent", searchContentType).build(),
        ParameterSpec.builder("limit", Int::class).build(),
      )
    )
    fileSpec.addFunction(placeholder("searchCount", Long::class.asTypeName(), defaultSearchContent))
    fileSpec.addFunction(placeholder("searchOne", entityType.copy(nullable = true), defaultSearchContent))
    fileSpec.addFunction(
      placeholder(
        "searchOneOrThrow",
        entityType,
        ParameterSpec.builder("errorCode", ErrorCode::class).build(),
        defaultSearchContent,
      )
    )
    fileSpec.addFunction(
      placeholder(
        "searchPage",
        Page::class.asClassName().parameterizedBy(entityType),
        ParameterSpec.builder("req", PagingAndSortingRequest::class).build(),
        defaultSearchContent,
      )
    )
    if (generateRemove) {
      fileSpec.addFunction(placeholder("remove", Long::class.asTypeName(), defaultSearchContent))
    }
  }

  private fun buildScopeExample(scopeClassName: ClassName, scopeFields: List<ScopeFieldInfo>): String {
    val requiredArgs = scopeFields.filterNot { it.nullable }.map { field ->
      when (field.operator) {
        ScopeOperator.IN, ScopeOperator.NOT_IN -> "${field.name} = ScopeFilter.All"
        ScopeOperator.IS_NULL, ScopeOperator.IS_NOT_NULL -> "${field.name} = false"
      }
    }
    val args = requiredArgs.ifEmpty {
      scopeFields.firstOrNull { it.operator == ScopeOperator.IN || it.operator == ScopeOperator.NOT_IN }
        ?.let { listOf("${it.name} = ScopeFilter.All") }
        .orEmpty()
    }
    return "${scopeClassName.simpleName}(${args.joinToString()})"
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

    scopeFields.forEach { field -> builder.addScopePredicate(field) }

    builder.addStatement("originalSearchContent()")
    builder.endControlFlow()
    builder.addStatement("return $returnStatement", *returnArgs)
    return builder.build()
  }

  private fun CodeBlock.Builder.addScopePredicate(field: ScopeFieldInfo) {
    if (field.operator == ScopeOperator.IS_NULL || field.operator == ScopeOperator.IS_NOT_NULL) {
      beginControlFlow("if (scope.${field.name} == true)")
      addStatement(
        "field<%T>(%S).${if (field.operator == ScopeOperator.IS_NULL) "isNull" else "isNotNull"}()",
        field.typeName,
        field.name,
      )
      endControlFlow()
      return
    }

    beginControlFlow("scope.${field.name}.let·{ scopeFilter ->")
    beginControlFlow("when (scopeFilter)")
    addStatement("%T.All -> Unit", ScopeFilter::class)
    beginControlFlow("is %T.Of ->", ScopeFilter::class)
    addStatement("val scopeValues = scopeFilter.values")
    beginControlFlow("if (scopeValues.isEmpty())")
    beginControlFlow("and")
    addStatement("field<%T>(%S).isNull()", field.typeName, field.name)
    addStatement("field<%T>(%S).isNotNull()", field.typeName, field.name)
    endControlFlow()
    nextControlFlow("else")
    addMembershipPredicate(field)
    endControlFlow()
    endControlFlow()
    endControlFlow()
    endControlFlow()
  }

  private fun CodeBlock.Builder.addMembershipPredicate(field: ScopeFieldInfo) {
    if (field.nullEquivalent == null) {
      val operation = if (field.operator == ScopeOperator.IN) "inList" else "notInList"
      addStatement("field<%T>(%S) $operation scopeValues", field.typeName, field.name)
      return
    }

    beginControlFlow("if (scopeValues.any { it.toString() == %S })", field.nullEquivalent)
    beginControlFlow(if (field.operator == ScopeOperator.IN) "or" else "and")
    val operation = if (field.operator == ScopeOperator.IN) "inList" else "notInList"
    addStatement("field<%T>(%S) $operation scopeValues", field.typeName, field.name)
    addStatement(
      "field<%T>(%S).${if (field.operator == ScopeOperator.IN) "isNull" else "isNotNull"}()",
      field.typeName,
      field.name,
    )
    endControlFlow()
    nextControlFlow("else")
    if (field.operator == ScopeOperator.IN) {
      addStatement("field<%T>(%S) inList scopeValues", field.typeName, field.name)
    } else {
      beginControlFlow("or")
      addStatement("field<%T>(%S) notInList scopeValues", field.typeName, field.name)
      addStatement("field<%T>(%S).isNull()", field.typeName, field.name)
      endControlFlow()
    }
    endControlFlow()
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
