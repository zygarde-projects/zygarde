package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.model.CrudOperationKind
import zygarde.codegen.model.CrudOperationToGenerateVo
import zygarde.codegen.model.CrudServiceImplToGenerateVo

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

  fun list(functionName: String) {
    operation(CrudOperationKind.LIST, functionName)
  }

  fun get(functionName: String, idParam: String) {
    operation(CrudOperationKind.GET, functionName, idParam)
  }

  inline fun <reified REQ : Any> create(functionName: String) {
    operation(CrudOperationKind.CREATE, functionName, requestType = REQ::class.asTypeName())
  }

  inline fun <reified REQ : Any> update(functionName: String, idParam: String) {
    operation(CrudOperationKind.UPDATE, functionName, idParam, REQ::class.asTypeName())
  }

  fun delete(functionName: String, idParam: String) {
    operation(CrudOperationKind.DELETE, functionName, idParam)
  }

  inline fun <reified REQ : Any> mergePatch(functionName: String, idParam: String) {
    operation(CrudOperationKind.MERGE_PATCH, functionName, idParam, REQ::class.asTypeName())
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
      operations = operations,
    )
  }

  @PublishedApi
  internal fun operation(
    kind: CrudOperationKind,
    functionName: String,
    idParam: String? = null,
    requestType: TypeName? = null,
  ) {
    operations.add(
      CrudOperationToGenerateVo(
        kind = kind,
        functionName = functionName,
        idParam = idParam,
        requestType = requestType,
      )
    )
  }

  private fun classNameFrom(name: String): ClassName {
    val normalizedName = name.trim()
    require(normalizedName.isNotEmpty()) {
      "Class name must not be blank."
    }
    return if ('.' in normalizedName) {
      ClassName.bestGuess(normalizedName)
    } else {
      ClassName(modelExtensionPackageName, normalizedName)
    }
  }
}
