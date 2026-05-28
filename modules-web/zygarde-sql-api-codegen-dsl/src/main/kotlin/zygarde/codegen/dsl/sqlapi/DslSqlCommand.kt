package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.asTypeName
import org.springframework.web.bind.annotation.RequestMethod
import kotlin.reflect.typeOf

class DslSqlCommand(
  @PublishedApi internal val functionName: String,
  private val path: String,
) {
  private var sqlLiteral: String? = null
  private var requestName: String? = null
  private var method: RequestMethod = RequestMethod.POST
  private var transactionPolicy: SqlApiTransactionPolicy? = null

  @PublishedApi internal var resultShape: SqlCommandResultShape = SqlCommandResultShape.AFFECTED_ROWS

  @PublishedApi internal var generatedKey: SqlGeneratedKeyToGenerateVo? = null

  @PublishedApi
  internal val declaredParams: MutableMap<String, SqlApiField> = linkedMapOf()

  fun sql(sql: String) {
    sqlLiteral = sql
  }

  inline fun <reified T> param(name: String, description: String = "") {
    declaredParams[name] = SqlApiField(name, typeOf<T>().asTypeName(), description)
  }

  inline fun <reified T> pathParam(name: String, description: String = "") {
    declaredParams[name] = SqlApiField(name, typeOf<T>().asTypeName(), description, SqlApiParamSource.PATH)
  }

  inline fun <reified T> queryParam(name: String, description: String = "") {
    declaredParams[name] = SqlApiField(name, typeOf<T>().asTypeName(), description, SqlApiParamSource.QUERY)
  }

  inline fun <reified T> bodyParam(name: String, description: String = "") {
    declaredParams[name] = SqlApiField(name, typeOf<T>().asTypeName(), description, SqlApiParamSource.BODY)
  }

  fun request(name: String) {
    requestName = name
  }

  fun transactional(enabled: Boolean = true, readOnly: Boolean = false) {
    transactionPolicy = when {
      !enabled -> SqlApiTransactionPolicy.NONE
      readOnly -> SqlApiTransactionPolicy.READ_ONLY
      else -> SqlApiTransactionPolicy.READ_WRITE
    }
  }

  fun method(method: RequestMethod) {
    this.method = method
  }

  fun post() {
    method = RequestMethod.POST
  }

  fun put() {
    method = RequestMethod.PUT
  }

  fun delete() {
    method = RequestMethod.DELETE
  }

  fun returnsAffectedRows() {
    resultShape = SqlCommandResultShape.AFFECTED_ROWS
    generatedKey = null
  }

  fun returnsNoContent() {
    resultShape = SqlCommandResultShape.NO_CONTENT
    generatedKey = null
  }

  inline fun <reified T> returnsGeneratedKey(
    name: String = "id",
    responseName: String = functionName.replaceFirstChar { it.uppercase() } + "KeyDto",
    keyColumnName: String = name,
    description: String = "",
  ) {
    resultShape = SqlCommandResultShape.GENERATED_KEY
    generatedKey = SqlGeneratedKeyToGenerateVo(
      responseName = responseName,
      field = SqlApiField(name, typeOf<T>().asTypeName(), description),
      keyColumnName = keyColumnName,
    )
  }

  fun toSqlCommandToGenerateVo(): SqlCommandToGenerateVo {
    val sql = requireNotNull(sqlLiteral) { "SQL is required for command '$functionName'" }
    val metadata = SqlCommandParser.parse(sql)
    val generatedKey = generatedKey
    if (resultShape == SqlCommandResultShape.GENERATED_KEY) {
      requireNotNull(generatedKey) { "Generated key configuration is required for command '$functionName'" }
      require(metadata.kind == SqlCommandKind.INSERT) {
        "SQL command '$functionName' can return generated keys only for INSERT statements"
      }
    }
    val defaultType = String::class.asTypeName().copy(nullable = true)
    val unusedDeclaredParamNames = declaredParams.keys - metadata.parameterNames.toSet()
    require(unusedDeclaredParamNames.isEmpty()) {
      "SQL command '$functionName' declares parameters that are not used by SQL: ${unusedDeclaredParamNames.joinToString()}"
    }
    val params = metadata.parameterNames.map { paramName ->
      declaredParams[paramName] ?: SqlApiField(paramName, defaultType)
    }
    validateBodyParams(params)
    validatePathParams(params)

    return SqlCommandToGenerateVo(
      functionName = functionName,
      path = path,
      sql = sql,
      method = method,
      requestName = requestName ?: functionName.replaceFirstChar { it.uppercase() } + "Req",
      params = params,
      resultShape = resultShape,
      generatedKey = generatedKey,
      transactionPolicy = transactionPolicy,
    )
  }

  private fun validateBodyParams(params: List<SqlApiField>) {
    val bodyParams = params.filter { it.source == SqlApiParamSource.BODY }
    require(bodyParams.isEmpty() || method in setOf(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH)) {
      "SQL command '$functionName' supports bodyParam only for POST, PUT, and PATCH"
    }
  }

  private fun validatePathParams(params: List<SqlApiField>) {
    val missingPathParamNames = params
      .filter { it.source == SqlApiParamSource.PATH }
      .map { it.name }
      .filterNot { path.containsPathVariable(it) }
    require(missingPathParamNames.isEmpty()) {
      "SQL command '$functionName' declares path parameters that are not present in path '$path': ${missingPathParamNames.joinToString()}"
    }
  }

  private fun String.containsPathVariable(name: String): Boolean {
    return Regex("""\{\s*${Regex.escape(name)}(?:\s*:[^}]*)?\s*}""").containsMatchIn(this)
  }
}
