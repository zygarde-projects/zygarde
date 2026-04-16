package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_INHERIT
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_PACKAGE
import zygarde.codegen.ZygardeJpaCodegenKaptOptions.DAO_SUFFIX
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.notNullTypeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortField
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.SearchSpecBuilder
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeJpaDaoExtensionGenerator(
  processingEnv: ProcessingEnvironment,
  val daoGenerateTo: String?,
) : AbstractZygardeGenerator(processingEnv) {
  private val daoInherit by lazy {
    processingEnv.options[DAO_INHERIT]
  }

  fun generateDaoExtensionsForEntityElements(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    val daoPackage = packageName(processingEnv.options.getOrDefault(DAO_PACKAGE, "data.dao"))
    val daoSuffix = processingEnv.options.getOrDefault(DAO_SUFFIX, "Dao")
    val folderToGenerate = daoGenerateTo?.let(::File) ?: folderToGenerate()
    val generateRemove = daoInherit?.contains("ZygardeEnhancedDao") == true

    elements.forEach { element ->
      val entityName = element.name()
      val daoName = "$entityName$daoSuffix"
      val entityType = element.notNullTypeName()
      val daoType = ClassName(daoPackage, daoName)
      val extensionFileName = "${daoName}Extensions"
      val searchContentType = searchContentLambdaType(entityType)
      val toSpringDataSort = MemberName("zygarde.data.jpa.search.request", "toSpringDataSort")
      val toSpringDataPageRequest = MemberName("zygarde.data.jpa.search.request", "toSpringDataPageRequest")

      val fileSpec = FileSpec.builder(daoPackage, extensionFileName)
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

      fileSpec.build().writeTo(folderToGenerate)
    }
  }

  private fun searchContentLambdaType(entityType: TypeName): LambdaTypeName {
    return LambdaTypeName.get(
      receiver = EnhancedSearch::class.asClassName().parameterizedBy(entityType),
      returnType = UNIT,
    )
  }
}
