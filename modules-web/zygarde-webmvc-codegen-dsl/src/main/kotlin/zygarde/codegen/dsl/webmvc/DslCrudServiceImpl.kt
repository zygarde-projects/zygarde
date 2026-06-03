package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.model.CrudOperationKind
import zygarde.codegen.model.CrudOperationToGenerateVo
import zygarde.codegen.model.CrudNotFoundToGenerateVo
import zygarde.codegen.model.CrudServiceImplToGenerateVo
import zygarde.codegen.model.CrudSoftDeleteTimestampToGenerateVo
import zygarde.codegen.model.CrudTransactionalToGenerateVo

class DslCrudServiceImpl(
  private val serviceName: String,
  private val entityType: TypeName,
  private val idType: TypeName,
) {
  private val modelExtensionPackageName = "zygarde.codegen.model.extensions"
  private var daoType: TypeName? = null
  private var daoPropertyName: String? = null
  private var dtoBuilderType: ClassName? = null
  private var applyExtensionsType: ClassName? = null
  private var patchExtensionsType: ClassName? = null
  private var transactional: CrudTransactionalToGenerateVo? = null
  private var notFound: CrudNotFoundToGenerateVo? = null
  private var softDeleteTimestamp: CrudSoftDeleteTimestampToGenerateVo? = null
  private val operations: MutableList<CrudOperationToGenerateVo> = mutableListOf()

  inline fun <reified DAO : Any> dao(propertyName: String) {
    setDao(propertyName, DAO::class.asTypeName())
  }

  @PublishedApi
  internal fun setDao(propertyName: String, type: TypeName) {
    daoPropertyName = propertyName
    daoType = type
  }

  inline fun <reified DTO : Any> dtoBuilder(builderName: String) {
    setDtoBuilder(builderName)
  }

  @PublishedApi
  internal fun setDtoBuilder(builderName: String) {
    dtoBuilderType = classNameFrom(builderName)
  }

  fun applyExtensions(extensionName: String) {
    applyExtensionsType = classNameFrom(extensionName)
  }

  fun patchExtensions(extensionName: String) {
    patchExtensionsType = classNameFrom(extensionName)
  }

  fun transactional(transactionManager: String? = null) {
    transactional = CrudTransactionalToGenerateVo(transactionManager)
  }

  inline fun <reified ERROR_CODE : Any> notFound(errorCodeName: String) {
    setNotFound(ERROR_CODE::class.asClassName(), errorCodeName)
  }

  @PublishedApi
  internal fun setNotFound(errorCodeType: ClassName, errorCodeName: String) {
    notFound = CrudNotFoundToGenerateVo(errorCodeType, normalizedName(errorCodeName, "Error code name"))
  }

  fun softDeleteTimestamp(fieldName: String) {
    softDeleteTimestamp = CrudSoftDeleteTimestampToGenerateVo(normalizedName(fieldName, "Soft delete field name"))
  }

  fun list(functionName: String, dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.LIST, functionName, dsl = dsl)
  }

  inline fun <reified REQ : Any> page(functionName: String, noinline dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.PAGE, functionName, requestType = REQ::class.asTypeName(), dsl = dsl)
  }

  fun get(functionName: String, idParam: String, dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.GET, functionName, idParam, dsl = dsl)
  }

  inline fun <reified REQ : Any> create(functionName: String, noinline dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.CREATE, functionName, requestType = REQ::class.asTypeName(), dsl = dsl)
  }

  inline fun <reified REQ : Any> update(functionName: String, idParam: String, noinline dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.UPDATE, functionName, idParam, REQ::class.asTypeName(), dsl)
  }

  fun delete(functionName: String, idParam: String, dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.DELETE, functionName, idParam, dsl = dsl)
  }

  inline fun <reified REQ : Any> mergePatch(functionName: String, idParam: String, noinline dsl: DslCrudOperation.() -> Unit = {}) {
    operation(CrudOperationKind.MERGE_PATCH, functionName, idParam, REQ::class.asTypeName(), dsl)
  }

  fun toCrudServiceImplToGenerateVo(): CrudServiceImplToGenerateVo {
    return CrudServiceImplToGenerateVo(
      serviceName = serviceName,
      entityType = entityType,
      idType = idType,
      daoType = requireNotNull(daoType) { "CRUD service impl '$serviceName' requires dao<T>(propertyName)." },
      daoPropertyName = requireNotNull(daoPropertyName) { "CRUD service impl '$serviceName' requires dao<T>(propertyName)." },
      dtoBuilderType = requireNotNull(dtoBuilderType) { "CRUD service impl '$serviceName' requires dtoBuilder<T>(builderName)." },
      applyExtensionsType = applyExtensionsType,
      patchExtensionsType = patchExtensionsType,
      transactional = transactional,
      notFound = notFound,
      softDeleteTimestamp = softDeleteTimestamp,
      operations = operations,
    )
  }

  @PublishedApi
  internal fun operation(
    kind: CrudOperationKind,
    functionName: String,
    idParam: String? = null,
    requestType: TypeName? = null,
    dsl: DslCrudOperation.() -> Unit = {},
  ) {
    val operationDsl = DslCrudOperation(kind, functionName).also(dsl)
    operations.add(
      CrudOperationToGenerateVo(
        kind = kind,
        functionName = functionName,
        idParam = idParam,
        requestType = requestType,
        daoMethod = operationDsl.daoMethod,
        notFound = operationDsl.notFound,
        hookType = operationDsl.hookType,
      )
    )
  }

  private fun classNameFrom(name: String): ClassName {
    val normalizedName = normalizedName(name, "Class name")
    return if ('.' in normalizedName) {
      ClassName.bestGuess(normalizedName)
    } else {
      ClassName(modelExtensionPackageName, normalizedName)
    }
  }

  private fun normalizedName(name: String, label: String): String {
    val normalizedName = name.trim()
    require(normalizedName.isNotEmpty()) {
      "$label must not be blank."
    }
    return normalizedName
  }
}

class DslCrudOperation internal constructor(
  private val kind: CrudOperationKind,
  private val functionName: String,
) {
  internal var daoMethod: String? = null
  internal var notFound: CrudNotFoundToGenerateVo? = null
  internal var hookType: ClassName? = null

  fun daoMethod(name: String) {
    daoMethod = normalizedName(name, "DAO method name")
  }

  inline fun <reified ERROR_CODE : Any> notFound(errorCodeName: String) {
    setNotFound(ERROR_CODE::class.asClassName(), errorCodeName)
  }

  @PublishedApi
  internal fun setNotFound(errorCodeType: ClassName, errorCodeName: String) {
    notFound = CrudNotFoundToGenerateVo(errorCodeType, normalizedName(errorCodeName, "Error code name"))
  }

  inline fun <reified HOOK : Any> hook() {
    setHook(HOOK::class.asClassName())
  }

  @PublishedApi
  internal fun setHook(type: ClassName) {
    require(kind in hookSupportedKinds) {
      "CRUD operation '$functionName' does not support hook."
    }
    require(hookType == null) {
      "CRUD operation '$functionName' declares duplicate hook."
    }
    hookType = type
  }

  private fun normalizedName(name: String, label: String): String {
    val normalizedName = name.trim()
    require(normalizedName.isNotEmpty()) {
      "$label must not be blank."
    }
    return normalizedName
  }

  private companion object {
    val hookSupportedKinds = setOf(
      CrudOperationKind.PAGE,
      CrudOperationKind.CREATE,
      CrudOperationKind.UPDATE,
      CrudOperationKind.DELETE,
    )
  }
}
