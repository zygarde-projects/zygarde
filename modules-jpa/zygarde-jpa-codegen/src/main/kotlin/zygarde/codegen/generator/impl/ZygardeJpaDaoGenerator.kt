package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
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
      val classBuilder = TypeSpec.classBuilder("Dao").addAnnotation(Component::class)
      val constructorBuilder = FunSpec.constructorBuilder()

      elements.sortedBy { it.typeName().toString() }.forEach {
        val daoFieldName = "${it.fieldName()}$daoSuffix"
        val daoClass = ClassName(daoPackage, "${it.name()}$daoSuffix")
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
        .writeTo(folderToGenerate)
    }
  }

  private data class ScopeFieldInfo(
    val name: String,
    val typeName: TypeName,
    val nullable: Boolean,
    val nullEquivalent: String?,
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
            val fieldTypeName = returnTypeMirror.kotlinTypeName(false)
            fields.putIfAbsent(
              propName,
              ScopeFieldInfo(
                name = propName,
                typeName = fieldTypeName,
                nullable = isNullable || nullEquivalent != null,
                nullEquivalent = nullEquivalent,
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

    // Primary constructor: all fields as Collection<T> (nullable if field is nullable on interface)
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

    // Convenience constructor: single values
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
      .addModifiers(com.squareup.kotlinpoet.KModifier.DATA)
      .primaryConstructor(primaryConstructor.build())
      .addProperties(properties)
      .addFunction(convenienceConstructor.build())
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

    scopeFields.forEach { field ->
      if (field.nullEquivalent != null) {
        // NullEquivalent field: generate OR IS NULL when null-equivalent value is present
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
