package zygarde.codegen.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.codegen.model.CrudOperationKind
import zygarde.codegen.model.CrudOperationToGenerateVo
import zygarde.codegen.model.CrudServiceImplToGenerateVo

internal class CrudServiceImplGenerator(
  private val apis: Collection<ApiToGenerateVo>
) {
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
      .build()

    val typeSpecBuilder = TypeSpec.classBuilder(implClassName)
      .addAnnotation(Service::class)
      .primaryConstructor(constructor)
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

    crudService.operations.forEach { operation ->
      val function = findServiceFunction(crudService.serviceName, operation)
      validateOperation(crudService, operation, function)
      typeSpecBuilder.addFunction(buildOperationFunction(crudService, operation, function))
    }

    return fileSpecBuilder
      .addType(typeSpecBuilder.build())
      .build()
  }

  private fun ApiToGenerateVo.findServiceFunction(serviceName: String, operation: CrudOperationToGenerateVo): ApiFunctionToGenerateVo {
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

    if (operation.kind in listOf(CrudOperationKind.GET, CrudOperationKind.UPDATE, CrudOperationKind.DELETE, CrudOperationKind.MERGE_PATCH)) {
      require(!operation.idParam.isNullOrBlank()) {
        "CRUD operation '${operation.functionName}' requires idParam."
      }
      require(function.pathVariables.containsKey(operation.idParam)) {
        "CRUD operation '${operation.functionName}' idParam '${operation.idParam}' is not declared as a path variable."
      }
    }

    if (operation.kind in listOf(CrudOperationKind.CREATE, CrudOperationKind.UPDATE, CrudOperationKind.MERGE_PATCH)) {
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

    if (operation.kind == CrudOperationKind.DELETE) {
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

    addServiceFunctionParameters(functionBuilder, function)

    function.responseType
      ?.generic(*function.responseTypeGenericArguments.toTypedArray())
      ?.let(functionBuilder::returns)

    when (operation.kind) {
      CrudOperationKind.LIST -> functionBuilder.addStatement(
        "return %N.buildAll(%N.findAll())",
        crudService.dtoAssemblerPropertyName(),
        crudService.daoPropertyName,
      )

      CrudOperationKind.GET -> functionBuilder.addStatement(
        "return %N.build(%N.getById(%N))",
        crudService.dtoAssemblerPropertyName(),
        crudService.daoPropertyName,
        operation.requireIdParam(),
      )

      CrudOperationKind.CREATE -> functionBuilder.addStatement(
        "return %N.build(%T().applyFrom(%N).let(%N::saveAndFlush))",
        crudService.dtoAssemblerPropertyName(),
        crudService.entityType,
        function.requestName,
        crudService.daoPropertyName,
      )

      CrudOperationKind.UPDATE -> functionBuilder.addStatement(
        "return %N.build(%N.getById(%N).applyFrom(%N).let(%N::saveAndFlush))",
        crudService.dtoAssemblerPropertyName(),
        crudService.daoPropertyName,
        operation.requireIdParam(),
        function.requestName,
        crudService.daoPropertyName,
      )

      CrudOperationKind.DELETE -> {
        functionBuilder.returns(UNIT)
        functionBuilder.addStatement(
          "%N.deleteById(%N)",
          crudService.daoPropertyName,
          operation.requireIdParam(),
        )
      }

      CrudOperationKind.MERGE_PATCH -> functionBuilder.addStatement(
        "return %N.build(%N.getById(%N).applyPatch(%N).let(%N::saveAndFlush))",
        crudService.dtoAssemblerPropertyName(),
        crudService.daoPropertyName,
        operation.requireIdParam(),
        function.requestName,
        crudService.daoPropertyName,
      )
    }

    return functionBuilder.build()
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

  private fun CrudServiceImplToGenerateVo.dtoAssemblerPropertyName(): String {
    return dtoAssemblerType.simpleName.replaceFirstChar { it.lowercase() }
  }
}
