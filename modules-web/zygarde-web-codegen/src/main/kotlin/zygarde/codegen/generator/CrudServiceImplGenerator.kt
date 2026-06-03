package zygarde.codegen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asTypeName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.codegen.model.CrudNotFoundToGenerateVo
import zygarde.codegen.model.CrudOperationKind
import zygarde.codegen.model.CrudOperationToGenerateVo
import zygarde.codegen.model.CrudServiceImplToGenerateVo
import zygarde.data.api.PageDto

internal class CrudServiceImplGenerator(
  private val apis: Collection<ApiToGenerateVo>
) {
  private val apiErrorCodeType = ClassName("zygarde.core.exception", "ApiErrorCode")
  private val businessExceptionType = ClassName("zygarde.core.exception", "BusinessException")
  private val localDateTimeType = ClassName("java.time", "LocalDateTime")
  private val transactionalType = ClassName("org.springframework.transaction.annotation", "Transactional")
  private val toSpringDataPageRequest = MemberName("zygarde.data.jpa.search.request", "toSpringDataPageRequest")

  fun generate(): List<FileSpec> {
    val crudServices = apis.flatMap { api ->
      api.crudServiceImpls.map { api to it }
    }
    val duplicateServiceNames = crudServices
      .groupBy { it.second.serviceName }
      .filterValues { it.size > 1 }
      .keys
    require(duplicateServiceNames.isEmpty()) {
      "CRUD service impl can only be declared once per serviceName. Duplicates: ${duplicateServiceNames.joinToString()}"
    }

    return crudServices.map { (api, crudService) ->
      api.generateCrudServiceImpl(crudService)
    }
  }

  private fun ApiToGenerateVo.generateCrudServiceImpl(crudService: CrudServiceImplToGenerateVo): FileSpec {
    val operationsByName = crudService.operations.associateBy { it.functionName }
    require(operationsByName.size == crudService.operations.size) {
      "CRUD service impl '${crudService.serviceName}' declares duplicate operation functionName."
    }
    validateRequiredExtensions(crudService)

    val implClassName = "${crudService.serviceName}Impl"
    val fileSpecBuilder = FileSpec.builder(serviceImplPackage, implClassName)
    crudService.applyExtensionsType?.let {
      fileSpecBuilder.addImport(it.packageName, "${it.simpleName}.applyFrom")
    }
    crudService.patchExtensionsType?.let {
      fileSpecBuilder.addImport(it.packageName, "${it.simpleName}.applyPatch")
    }
    if (crudService.operations.any { it.kind == CrudOperationKind.PAGE && it.daoMethod.isNullOrBlank() }) {
      fileSpecBuilder.addImport(toSpringDataPageRequest.packageName, toSpringDataPageRequest.simpleName)
    }

    val constructor = FunSpec.constructorBuilder()
      .addParameter(
        ParameterSpec.builder(crudService.daoPropertyName, crudService.daoType)
          .addAnnotation(Autowired::class)
          .build()
      )
      .addParameter(
        ParameterSpec.builder(crudService.dtoAssemblerPropertyName(), crudService.dtoAssemblerType)
          .addAnnotation(Autowired::class)
          .build()
      )
    val hookProperties = crudService.hookProperties()
    hookProperties.forEach { hook ->
      constructor.addParameter(
        ParameterSpec.builder(hook.propertyName, hook.type)
          .addAnnotation(Autowired::class)
          .build()
      )
    }

    val typeSpecBuilder = TypeSpec.classBuilder(implClassName)
      .addAnnotation(Service::class)
      .primaryConstructor(constructor.build())
      .addProperty(
        PropertySpec.builder(crudService.daoPropertyName, crudService.daoType, KModifier.PRIVATE)
          .initializer(crudService.daoPropertyName)
          .build()
      )
      .addProperty(
        PropertySpec.builder(crudService.dtoAssemblerPropertyName(), crudService.dtoAssemblerType, KModifier.PRIVATE)
          .initializer(crudService.dtoAssemblerPropertyName())
          .build()
      )
      .addSuperinterface(ClassName(serviceInterfacePackage, crudService.serviceName))
    hookProperties.forEach { hook ->
      typeSpecBuilder.addProperty(
        PropertySpec.builder(hook.propertyName, hook.type, KModifier.PRIVATE)
          .initializer(hook.propertyName)
          .build()
      )
    }

    crudService.operations.forEach { operation ->
      val function = findServiceFunction(crudService.serviceName, operation)
      validateOperation(crudService, operation, function)
      typeSpecBuilder.addFunction(buildOperationFunction(crudService, operation, function))
    }

    return fileSpecBuilder
      .addType(typeSpecBuilder.build())
      .build()
  }

  private fun ApiToGenerateVo.findServiceFunction(
    serviceName: String,
    operation: CrudOperationToGenerateVo
  ): ApiFunctionToGenerateVo {
    return functions.firstOrNull { function ->
      effectiveServiceName(function) == serviceName && effectiveServiceFunctionName(function) == operation.functionName
    } ?: error(
      "CRUD service impl '$serviceName' references function '${operation.functionName}', but no matching service interface function exists."
    )
  }

  private fun ApiToGenerateVo.effectiveServiceName(function: ApiFunctionToGenerateVo): String {
    return function.serviceName ?: "${apiName}Service"
  }

  private fun effectiveServiceFunctionName(function: ApiFunctionToGenerateVo): String {
    return function.serviceFunctionName ?: function.functionName
  }

  private fun validateRequiredExtensions(crudService: CrudServiceImplToGenerateVo) {
    val requiresApplyExtensions = crudService.operations.any {
      it.kind == CrudOperationKind.CREATE || it.kind == CrudOperationKind.UPDATE
    }
    require(!requiresApplyExtensions || crudService.applyExtensionsType != null) {
      "CRUD service impl '${crudService.serviceName}' requires applyExtensions for create/update operations."
    }

    val requiresPatchExtensions = crudService.operations.any { it.kind == CrudOperationKind.MERGE_PATCH }
    require(!requiresPatchExtensions || crudService.patchExtensionsType != null) {
      "CRUD service impl '${crudService.serviceName}' requires patchExtensions for merge-patch operations."
    }
  }

  private fun validateOperation(
    crudService: CrudServiceImplToGenerateVo,
    operation: CrudOperationToGenerateVo,
    function: ApiFunctionToGenerateVo
  ) {
    require(!function.postProcessing) {
      "CRUD service impl '${crudService.serviceName}' does not support postProcessing function '${operation.functionName}' in v1."
    }
    require(function.authenticationDetailType == null) {
      "CRUD service impl '${crudService.serviceName}' does not support authentication detail function '${operation.functionName}' in v1."
    }

    require(
      operation.hookType == null ||
        operation.kind in listOf(CrudOperationKind.PAGE, CrudOperationKind.CREATE, CrudOperationKind.UPDATE, CrudOperationKind.DELETE)
    ) {
      "CRUD operation '${operation.functionName}' does not support hook."
    }

    if (operation.kind in listOf(CrudOperationKind.GET, CrudOperationKind.UPDATE, CrudOperationKind.DELETE, CrudOperationKind.MERGE_PATCH)) {
      require(!operation.idParam.isNullOrBlank()) {
        "CRUD operation '${operation.functionName}' requires idParam."
      }
      require(function.pathVariables.containsKey(operation.idParam)) {
        "CRUD operation '${operation.functionName}' idParam '${operation.idParam}' is not declared as a path variable."
      }
    }

    if (operation.kind in listOf(CrudOperationKind.PAGE, CrudOperationKind.CREATE, CrudOperationKind.UPDATE, CrudOperationKind.MERGE_PATCH)) {
      require(function.requestType != null) {
        "CRUD operation '${operation.functionName}' requires a request DTO."
      }
      operation.requestType?.let { requestType ->
        require(requestType == function.requestType) {
          "CRUD operation '${operation.functionName}' request DTO '$requestType' does not match " +
            "service function request DTO '${function.requestType}'."
        }
      }
    }

    if (operation.kind == CrudOperationKind.PAGE) {
      require(function.responseType == PageDto::class.asTypeName()) {
        "CRUD page operation '${operation.functionName}' must return PageDto."
      }
      require(function.responseTypeGenericArguments.size == 1) {
        "CRUD page operation '${operation.functionName}' must declare a PageDto item type."
      }
    } else if (operation.kind == CrudOperationKind.DELETE) {
      require(function.responseType == null) {
        "CRUD delete operation '${operation.functionName}' must not declare a response DTO."
      }
    } else {
      require(function.responseType != null) {
        "CRUD operation '${operation.functionName}' requires a response DTO."
      }
    }
  }

  private fun buildOperationFunction(
    crudService: CrudServiceImplToGenerateVo,
    operation: CrudOperationToGenerateVo,
    function: ApiFunctionToGenerateVo
  ): FunSpec {
    val functionBuilder = FunSpec.builder(effectiveServiceFunctionName(function))
      .addModifiers(KModifier.OVERRIDE)
    crudService.transactional?.let { transactional ->
      val annotation = AnnotationSpec.builder(transactionalType)
      transactional.transactionManager?.let {
        annotation.addMember("transactionManager = %S", it)
      }
      functionBuilder.addAnnotation(annotation.build())
    }

    addServiceFunctionParameters(functionBuilder, function)

    function.responseType
      ?.generic(*function.responseTypeGenericArguments.toTypedArray())
      ?.let(functionBuilder::returns)

    when (operation.kind) {
      CrudOperationKind.LIST -> functionBuilder.addStatement(
        "return %N.buildAll(%N.%N())",
        crudService.dtoAssemblerPropertyName(),
        crudService.daoPropertyName,
        operation.daoMethod ?: "findAll",
      )

      CrudOperationKind.PAGE -> addPageStatements(functionBuilder, crudService, operation, function)

      CrudOperationKind.GET -> functionBuilder.addStatement(
        "return %N.build(%L)",
        crudService.dtoAssemblerPropertyName(),
        crudService.findEntity(operation),
      )

      CrudOperationKind.CREATE -> addCreateStatements(functionBuilder, crudService, operation, function)

      CrudOperationKind.UPDATE -> addUpdateStatements(functionBuilder, crudService, operation, function)

      CrudOperationKind.DELETE -> addDeleteStatements(functionBuilder, crudService, operation)

      CrudOperationKind.MERGE_PATCH -> functionBuilder.addStatement(
        "return %N.build(%L.applyPatch(%N).let(%N::saveAndFlush))",
        crudService.dtoAssemblerPropertyName(),
        crudService.findEntity(operation),
        function.requestName,
        crudService.daoPropertyName,
      )
    }

    return functionBuilder.build()
  }

  private fun addPageStatements(
    functionBuilder: FunSpec.Builder,
    crudService: CrudServiceImplToGenerateVo,
    operation: CrudOperationToGenerateVo,
    function: ApiFunctionToGenerateVo
  ) {
    operation.hookType?.let {
      functionBuilder.addStatement("%N.beforePage(%N)", it.hookPropertyName(), function.requestName)
    }
    if (operation.daoMethod.isNullOrBlank()) {
      functionBuilder.addStatement(
        "val page = %N.findAll(%N.%M())",
        crudService.daoPropertyName,
        function.requestName,
        toSpringDataPageRequest
      )
    } else {
      functionBuilder.addStatement("val page = %N.%N(%N)", crudService.daoPropertyName, operation.daoMethod, function.requestName)
    }
    functionBuilder.addStatement("val items = %N.buildAll(page.content).toList()", crudService.dtoAssemblerPropertyName())
    operation.hookType?.let {
      functionBuilder.addStatement("%N.afterPage(%N)", it.hookPropertyName(), function.requestName)
    }
    functionBuilder.addStatement("return %T(page.number + 1, page.totalPages, items, page.totalElements)", PageDto::class)
  }

  private fun addCreateStatements(
    functionBuilder: FunSpec.Builder,
    crudService: CrudServiceImplToGenerateVo,
    operation: CrudOperationToGenerateVo,
    function: ApiFunctionToGenerateVo
  ) {
    functionBuilder.addStatement("val entity = %T().applyFrom(%N)", crudService.entityType, function.requestName)
    operation.hookType?.let {
      functionBuilder.addStatement("%N.beforeCreate(entity, %N)", it.hookPropertyName(), function.requestName)
    }
    functionBuilder.addStatement("val saved = %N.saveAndFlush(entity)", crudService.daoPropertyName)
    operation.hookType?.let {
      functionBuilder.addStatement("%N.afterCreate(saved, %N)", it.hookPropertyName(), function.requestName)
    }
    functionBuilder.addStatement("return %N.build(saved)", crudService.dtoAssemblerPropertyName())
  }

  private fun addUpdateStatements(
    functionBuilder: FunSpec.Builder,
    crudService: CrudServiceImplToGenerateVo,
    operation: CrudOperationToGenerateVo,
    function: ApiFunctionToGenerateVo
  ) {
    functionBuilder.addStatement("val entity = %L.applyFrom(%N)", crudService.findEntity(operation), function.requestName)
    operation.hookType?.let {
      functionBuilder.addStatement(
        "%N.beforeUpdate(entity, %N, %N)",
        it.hookPropertyName(),
        operation.requireIdParam(),
        function.requestName
      )
    }
    functionBuilder.addStatement("val saved = %N.saveAndFlush(entity)", crudService.daoPropertyName)
    operation.hookType?.let {
      functionBuilder.addStatement(
        "%N.afterUpdate(saved, %N, %N)",
        it.hookPropertyName(),
        operation.requireIdParam(),
        function.requestName
      )
    }
    functionBuilder.addStatement("return %N.build(saved)", crudService.dtoAssemblerPropertyName())
  }

  private fun addDeleteStatements(
    functionBuilder: FunSpec.Builder,
    crudService: CrudServiceImplToGenerateVo,
    operation: CrudOperationToGenerateVo
  ) {
    functionBuilder.returns(UNIT)
    functionBuilder.addStatement("val entity = %L", crudService.findEntity(operation))
    operation.hookType?.let {
      functionBuilder.addStatement("%N.beforeDelete(entity, %N)", it.hookPropertyName(), operation.requireIdParam())
    }
    if (crudService.softDeleteTimestamp == null) {
      functionBuilder.addStatement("%N.delete(entity)", crudService.daoPropertyName)
      operation.hookType?.let {
        functionBuilder.addStatement("%N.afterDelete(entity, %N)", it.hookPropertyName(), operation.requireIdParam())
      }
    } else {
      functionBuilder.addStatement("entity.%N = %T.now()", crudService.softDeleteTimestamp.fieldName, localDateTimeType)
      functionBuilder.addStatement("val saved = %N.saveAndFlush(entity)", crudService.daoPropertyName)
      operation.hookType?.let {
        functionBuilder.addStatement("%N.afterDelete(saved, %N)", it.hookPropertyName(), operation.requireIdParam())
      }
    }
  }

  private fun addServiceFunctionParameters(functionBuilder: FunSpec.Builder, function: ApiFunctionToGenerateVo) {
    function.pathVariables.forEach { (name, type) ->
      functionBuilder.addParameter(name, type)
    }
    function.requestParams.forEach { (name, type) ->
      functionBuilder.addParameter(name, type)
    }
    function.requestType
      ?.generic(*function.requestTypeGenericArguments.toTypedArray())
      ?.let { requestType ->
        functionBuilder.addParameter(function.requestName, requestType)
      }
  }

  private fun CrudOperationToGenerateVo.requireIdParam(): String {
    return requireNotNull(idParam) {
      "CRUD operation '$functionName' requires idParam."
    }
  }

  private fun CrudServiceImplToGenerateVo.findEntity(operation: CrudOperationToGenerateVo): CodeBlock {
    return CodeBlock.of(
      "%N.%N(%N).orElseThrow·{·%T(%L)·}",
      daoPropertyName,
      operation.daoMethod ?: "findById",
      operation.requireIdParam(),
      businessExceptionType,
      notFoundCode(operation),
    )
  }

  private fun CrudServiceImplToGenerateVo.notFoundCode(operation: CrudOperationToGenerateVo): CodeBlock {
    val notFound = operation.notFound ?: notFound ?: CrudNotFoundToGenerateVo(apiErrorCodeType, "NOT_FOUND")
    return CodeBlock.of("%T.%L", notFound.errorCodeType, notFound.errorCodeName)
  }

  private fun CrudServiceImplToGenerateVo.dtoAssemblerPropertyName(): String {
    return dtoAssemblerType.simpleName.replaceFirstChar { it.lowercase() }
  }

  private fun CrudServiceImplToGenerateVo.hookProperties(): List<HookProperty> {
    val hooks = operations.mapNotNull { it.hookType }.distinct()
    val duplicatedPropertyNames = hooks
      .groupBy { it.hookPropertyName() }
      .filterValues { it.size > 1 }
      .keys
    require(duplicatedPropertyNames.isEmpty()) {
      "CRUD service impl '$serviceName' declares hooks with duplicate property names: ${duplicatedPropertyNames.joinToString()}."
    }
    return hooks.map { HookProperty(it.hookPropertyName(), it) }
  }

  private fun ClassName.hookPropertyName(): String {
    return simpleName.replaceFirstChar { it.lowercase() }
  }

  private data class HookProperty(
    val propertyName: String,
    val type: ClassName,
  )
}
