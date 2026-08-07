package zygarde.codegen.generator.impl

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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Component
import zygarde.codegen.NullEquivalent
import zygarde.codegen.ScopeMarker
import zygarde.codegen.ScopeOp
import zygarde.codegen.ScopeOperator
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_COMBINE
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_INHERIT
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_PACKAGE
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_SUFFIX
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.allSuperTypes
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.notNullTypeName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.resolveGenericFieldTypeMap
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.extension.kotlinpoet.kotlinTypeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.core.exception.BusinessException
import zygarde.core.exception.CommonErrorCode
import zygarde.core.exception.ErrorCode
import zygarde.core.extension.exception.errWhenNull
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortField
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.SearchSpecBuilder
import zygarde.data.jpa.search.ScopeFilter
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.ExecutableType
import jakarta.persistence.Id
import jakarta.persistence.IdClass

class ZygardeJpaDaoGenerator(
  processingEnv: ProcessingEnvironment,
  val daoGenerateTo: String?,
) : AbstractZygardeGenerator(processingEnv) {
  private val daoInherit by lazy {
    processingEnv.options[DAO_INHERIT]
  }

  fun generateDaoForEntityElements(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    val daoPackage = packageName(processingEnv.options.getOrDefault(DAO_PACKAGE, "data.dao"))
    val daoSuffix = processingEnv.options.getOrDefault(DAO_SUFFIX, "Dao")
    val folderToGenerate = daoGenerateTo?.let(::File) ?: folderToGenerate()
    val generateRemove = daoInherit?.contains("ZygardeEnhancedDao") == true
    val toSpringDataSort = MemberName("zygarde.data.jpa.search.request", "toSpringDataSort")
    val toSpringDataPageRequest = MemberName("zygarde.data.jpa.search.request", "toSpringDataPageRequest")
    elements.map { element ->
      "${element.name()}$daoSuffix".also { daoName ->
        val entityType = element.notNullTypeName()
        val daoType = ClassName(daoPackage, daoName)
        FileSpec.builder(daoPackage, daoName)
          .addType(
            TypeSpec.interfaceBuilder(daoName)
              .also { interfaceBuilder ->
                val superInterface = daoInherit?.let { ClassName.bestGuess(it) }
                if (superInterface != null) {
                  interfaceBuilder.addSuperinterface(
                    superInterface.generic(entityType, element.findIdClass())
                  )
                } else {
                  interfaceBuilder.addSuperinterface(
                    JpaRepository::class.generic(entityType, element.findIdClass())
                  )
                    .addSuperinterface(
                      JpaSpecificationExecutor::class.generic(entityType)
                    )
                }
              }
              .build()
          )
          .build()
          .writeTo(folderToGenerate)

        // Detect scope fields from @ScopeMarker interfaces
        val scopeFields = collectScopeFields(element)
        val scopeClassName = if (scopeFields.isNotEmpty()) {
          val scopeType = ClassName(daoPackage, "${element.name()}Scope")
          generateScopeDataClass(daoPackage, element.name().toString(), scopeFields, folderToGenerate)
          scopeType
        } else {
          null
        }
        val searchContentType = searchContentLambdaType(entityType)
        buildExtensionFileSpec(
          daoPackage,
          daoName,
          daoType,
          entityType,
          searchContentType,
          toSpringDataSort,
          toSpringDataPageRequest,
          generateRemove,
          scopeClassName,
          scopeFields,
        ).build().writeTo(folderToGenerate)
      }
    }

    if (processingEnv.options.getOrDefault(DAO_COMBINE, "true") == "true") {
      // Field injection instead of constructor injection: JVM caps method parameters at 255
      // slots and the reflective instantiation path trips a few slots earlier, so schemas
      // with ~250 DAOs could no longer boot with a generated constructor.
      val classBuilder = TypeSpec.classBuilder("Dao").addAnnotation(Component::class)

      elements.sortedBy { it.typeName().toString() }.forEach {
        val daoFieldName = "${it.fieldName()}$daoSuffix"
        val daoClass = ClassName(daoPackage, "${it.name()}$daoSuffix")
        classBuilder.addProperty(
          PropertySpec
            .builder(daoFieldName, daoClass)
            .mutable()
            .addModifiers(KModifier.LATEINIT)
            .addAnnotation(Autowired::class)
            .build()
        )
      }

      FileSpec.builder(daoPackage, "Dao")
        .addType(classBuilder.build())
        .build()
        .writeTo(folderToGenerate)
    }
  }

  private data class ScopeFieldInfo(
    val name: String,
    val typeName: TypeName,
    val nullable: Boolean,
    val nullEquivalent: String?,
    val operator: ScopeOperator,
  )

  private fun collectScopeFields(element: Element): List<ScopeFieldInfo> {
    val fields = mutableMapOf<String, ScopeFieldInfo>()
    element.allSuperTypes(processingEnv).forEach { superType ->
      val annotation = superType.getAnnotation(ScopeMarker::class.java)
      if (annotation != null) {
        superType.enclosedElements
          .filter { it.kind == ElementKind.METHOD }
          .filter {
            val name = it.simpleName.toString()
            name.startsWith("get") || name.startsWith("is")
          }
          .forEach eachMethod@{ method ->
            val methodName = method.simpleName.toString()
            val propName = if (methodName.startsWith("is")) {
              methodName.removePrefix("is").replaceFirstChar { it.lowercase() }
            } else {
              methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
            }
            val executableType = method.asType() as? ExecutableType ?: return@eachMethod
            val returnTypeMirror = executableType.returnType
            val isNullable = method.getAnnotation(org.jetbrains.annotations.Nullable::class.java) != null
            val nullEquivalent = method.getAnnotation(NullEquivalent::class.java)?.value
              ?: method.annotationMirrors.find {
                it.annotationType.asElement().simpleName.toString() == "NullEquivalent"
              }?.elementValues?.entries?.find {
                it.key.simpleName.toString() == "value"
              }?.value?.value as? String
            val operator = method.getAnnotation(ScopeOp::class.java)?.value
              ?: method.annotationMirrors.find {
                it.annotationType.asElement().simpleName.toString() == "ScopeOp"
              }?.elementValues?.entries?.find {
                it.key.simpleName.toString() == "value"
              }?.value?.value?.toString()?.let(ScopeOperator::valueOf)
              ?: ScopeOperator.IN
            val isMembership = operator == ScopeOperator.IN || operator == ScopeOperator.NOT_IN
            val fieldTypeName = returnTypeMirror.kotlinTypeName(false)
            fields.putIfAbsent(
              propName,
              ScopeFieldInfo(
                name = propName,
                typeName = fieldTypeName,
                nullable = isNullable || (isMembership && nullEquivalent != null),
                nullEquivalent = nullEquivalent.takeIf { isMembership },
                operator = operator,
              )
            )
          }
      }
    }
    return fields.values.toList()
  }

  private fun generateScopeDataClass(
    daoPackage: String,
    entityName: String,
    fields: List<ScopeFieldInfo>,
    folderToGenerate: File,
  ) {
    val scopeClassName = "${entityName}Scope"
    val collectionType = Collection::class.asClassName()
    val javaCollectionType = ClassName("java.util", "Collection")
    val scopeFilterType = ScopeFilter::class.asClassName()

    // Primary constructor: membership fields use ScopeFilter<T>; null-check operators use Boolean.
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

    // Compatibility constructor: collections used by the original generated API.
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

    // Convenience constructor: single values
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
      .addModifiers(com.squareup.kotlinpoet.KModifier.DATA)
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
      .writeTo(folderToGenerate)
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
      // Scoped entity — generate extension functions with scope parameter
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
      // Non-scoped entity — generate extension functions without scope parameter (unchanged)
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

  /**
   * Builds a code block that wraps the original searchContent lambda with scope predicates.
   *
   * The generated code rewrites the searchContent lambda to prepend scope field predicates:
   * ```
   * val originalSearchContent = searchContent
   * val searchContent: EnhancedSearch<Entity>.() -> Unit = {
   *   scope.fieldA.let { field<Type>("fieldA") inList it }
   *   scope.fieldB?.let { field<Type>("fieldB") inList it }
   *   originalSearchContent()
   * }
   * return findAll(SearchSpecBuilder.buildSpec(searchContent))
   * ```
   */
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

  private fun Element.findIdClass(): TypeName {
    val idClassAnnotation = this.getAnnotation(IdClass::class.java)
    if (idClassAnnotation != null) {
      return safeGetTypeFromAnnotation { idClassAnnotation.value.asTypeName() }.kotlin(canBeNullable = false)
    }
    val allFieldsIncludeSuper = this.allFieldsIncludeSuper()
    val idElement = allFieldsIncludeSuper
      .find { it.getAnnotation(Id::class.java) != null }
      .errWhenNull(CommonErrorCode.ERROR, "no id class found for entity ${this.simpleName}")
    val genericFieldTypeMap = this.resolveGenericFieldTypeMap(processingEnv)
    val idTypeMirror = idElement.asType()
    val idFieldLocation = "${idElement.enclosingElement}_$idTypeMirror"
    return genericFieldTypeMap.getOrElse(idFieldLocation) {
      idTypeMirror.kotlinTypeName(false)
    }
  }
}
