package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.asTypeName
import zygarde.sql.api.SqlApiContextParamResolver
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

class DslSqlQuery(
  private val functionName: String,
  private val path: String,
) {
  private var sqlLiteral: String? = null
  private var requestName: String? = null
  private var responseName: String? = null
  private var resultShape: SqlQueryResultShape = SqlQueryResultShape.LIST
  private var page: SqlApiPageToGenerateVo? = null
  private var transactionPolicy: SqlApiTransactionPolicy? = null

  @PublishedApi
  internal val declaredParams: MutableMap<String, SqlApiField> = linkedMapOf()

  @PublishedApi
  internal val declaredColumns: MutableMap<String, SqlApiField> = linkedMapOf()

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

  inline fun <reified T, reified R : SqlApiContextParamResolver<T>> contextParam(name: String, description: String = "") {
    declaredParams[name] = SqlApiField(
      name = name,
      type = typeOf<T>().asTypeName(),
      description = description,
      source = SqlApiParamSource.CONTEXT,
      contextValueSource = SqlApiContextValueSource.ResolverByType(typeOf<R>().asTypeName()),
    )
  }

  inline fun <reified T> contextParam(
    name: String,
    resolver: KClass<*>,
    description: String = "",
  ) {
    require(SqlApiContextParamResolver::class.java.isAssignableFrom(resolver.java)) {
      "Context parameter '$name' resolver must implement SqlApiContextParamResolver"
    }
    declaredParams[name] = SqlApiField(
      name = name,
      type = typeOf<T>().asTypeName(),
      description = description,
      source = SqlApiParamSource.CONTEXT,
      contextValueSource = SqlApiContextValueSource.ResolverByType(resolver.asTypeName()),
    )
  }

  inline fun <reified T> contextParam(name: String, resolverBeanName: String, description: String = "") {
    require(resolverBeanName.isNotBlank()) { "Context parameter '$name' must declare a resolver bean name" }
    declaredParams[name] = SqlApiField(
      name = name,
      type = typeOf<T>().asTypeName(),
      description = description,
      source = SqlApiParamSource.CONTEXT,
      contextValueSource = SqlApiContextValueSource.ResolverByName(resolverBeanName),
    )
  }

  inline fun <reified T> column(name: String, description: String = "") {
    declaredColumns[name] = SqlApiField(name, typeOf<T>().asTypeName(), description)
  }

  fun request(name: String) {
    requestName = name
  }

  fun response(name: String) {
    responseName = name
  }

  fun transactional(enabled: Boolean = true, readOnly: Boolean = true) {
    transactionPolicy = when {
      !enabled -> SqlApiTransactionPolicy.NONE
      readOnly -> SqlApiTransactionPolicy.READ_ONLY
      else -> SqlApiTransactionPolicy.READ_WRITE
    }
  }

  fun returnsList() {
    resultShape = SqlQueryResultShape.LIST
    page = null
  }

  fun returnsOne() {
    resultShape = SqlQueryResultShape.ONE
    page = null
  }

  fun returnsOneOrNull() {
    resultShape = SqlQueryResultShape.ONE_NULLABLE
    page = null
  }

  fun returnsPage(
    countSql: String,
    countColumnName: String = "totalCount",
    pageParamName: String = "atPage",
    pageSizeParamName: String = "pageSize",
    offsetParamName: String = "offset",
  ) {
    resultShape = SqlQueryResultShape.PAGE
    page = SqlApiPageToGenerateVo(
      countSql = countSql,
      countColumnName = countColumnName,
      pageParamName = pageParamName,
      pageSizeParamName = pageSizeParamName,
      offsetParamName = offsetParamName,
    )
  }

  fun toSqlQueryToGenerateVo(): SqlQueryToGenerateVo {
    val sql = requireNotNull(sqlLiteral) { "SQL is required for query '$functionName'" }
    val metadata = SqlSelectParser.parse(sql)
    val page = page
    val countMetadata = page?.let { SqlSelectParser.parse(it.countSql) }
    if (resultShape == SqlQueryResultShape.PAGE) {
      requireNotNull(page) { "Page SQL configuration is required for query '$functionName'" }
      require(metadata.parameterNames.contains(page.offsetParamName)) {
        "Page SQL query '$functionName' must use offset parameter ':${page.offsetParamName}'"
      }
      require(metadata.parameterNames.contains(page.pageSizeParamName)) {
        "Page SQL query '$functionName' must use page size parameter ':${page.pageSizeParamName}'"
      }
      require(countMetadata?.columnAliases?.contains(page.countColumnName) == true) {
        "Page count SQL query '$functionName' must select '${page.countColumnName}'"
      }
    }
    val defaultType = String::class.asTypeName().copy(nullable = true)
    val defaultPageNumberType = Int::class.asTypeName()
    val sqlParamNames = (metadata.parameterNames + countMetadata.orEmptyParameterNames()).distinct()
    val requestParamNames = sqlParamNames
      .filterNot { page?.offsetParamName == it }
      .toMutableList()
      .also { paramNames ->
        page?.let {
          paramNames.addIfAbsent(it.pageParamName)
          paramNames.addIfAbsent(it.pageSizeParamName)
        }
      }
    val supportedDeclaredParamNames = requestParamNames.toSet()
    val unusedDeclaredParamNames = declaredParams.keys - supportedDeclaredParamNames
    require(unusedDeclaredParamNames.isEmpty()) {
      "SQL query '$functionName' declares parameters that are not used by SQL: ${unusedDeclaredParamNames.joinToString()}"
    }

    val params = requestParamNames.map { paramName ->
      declaredParams[paramName]
        ?: if (page?.pageParamName == paramName || page?.pageSizeParamName == paramName) {
          SqlApiField(paramName, defaultPageNumberType)
        } else {
          SqlApiField(paramName, defaultType)
        }
    }
    val bodyParams = params.filter { it.source == SqlApiParamSource.BODY }
    require(bodyParams.isEmpty()) {
      "SQL query '$functionName' does not support bodyParam; use param, queryParam, pathParam, or contextParam"
    }
    validatePathParams(functionName, path, params)
    val unusedDeclaredColumnNames = declaredColumns.keys - metadata.columnAliases.toSet()
    require(unusedDeclaredColumnNames.isEmpty()) {
      "SQL query '$functionName' declares columns that are not selected by SQL: ${unusedDeclaredColumnNames.joinToString()}"
    }
    val columns = metadata.columnAliases.map { columnName ->
      declaredColumns[columnName] ?: SqlApiField(columnName, defaultType)
    }
    require(columns.isNotEmpty()) {
      "SQL query '$functionName' must declare at least one output column or use SELECT expressions with AS aliases"
    }

    return SqlQueryToGenerateVo(
      functionName = functionName,
      path = path,
      sql = sql,
      requestName = requestName ?: functionName.replaceFirstChar { it.uppercase() } + "Req",
      responseName = responseName ?: functionName.replaceFirstChar { it.uppercase() } + "Dto",
      params = params,
      columns = columns,
      resultShape = resultShape,
      page = page,
      transactionPolicy = transactionPolicy,
    )
  }

  private fun SqlSelectMetadata?.orEmptyParameterNames(): List<String> {
    return this?.parameterNames ?: emptyList()
  }

  private fun MutableList<String>.addIfAbsent(value: String) {
    if (!contains(value)) {
      add(value)
    }
  }

  private fun validatePathParams(functionName: String, path: String, params: List<SqlApiField>) {
    val missingPathParamNames = params
      .filter { it.source == SqlApiParamSource.PATH }
      .map { it.name }
      .filterNot { path.containsPathVariable(it) }
    require(missingPathParamNames.isEmpty()) {
      "SQL query '$functionName' declares path parameters that are not present in path '$path': ${missingPathParamNames.joinToString()}"
    }
  }

  private fun String.containsPathVariable(name: String): Boolean {
    return Regex("""\{\s*${Regex.escape(name)}(?:\s*:[^}]*)?\s*}""").containsMatchIn(this)
  }
}
