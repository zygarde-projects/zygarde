package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Component
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_COMBINE
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_INHERIT
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_PACKAGE
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_SUFFIX
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
import javax.persistence.Id
import javax.persistence.IdClass

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

  private fun buildExtensionFileSpec(
    daoPackage: String,
    daoName: String,
    daoType: ClassName,
    entityType: TypeName,
    searchContentType: LambdaTypeName,
    toSpringDataSort: MemberName,
    toSpringDataPageRequest: MemberName,
    generateRemove: Boolean,
  ): FileSpec.Builder {
    val fileSpec = FileSpec.builder(daoPackage, "${daoName}Extensions")
      .addFunction(
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
            "return sorts?.let { findAll(%T.buildSpec(searchContent), it.%M()) } ?: search(searchContent)",
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
            "return findOne(%T.buildSpec(searchContent)).let { if (it.isPresent) it.get() else null }",
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
          .returns(Int::class)
          .addStatement("return delete(%T.buildSpec(searchContent))", SearchSpecBuilder::class)
          .build()
      )
    }

    return fileSpec
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
