package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.typeOf

class DslSqlQuery(
  private val functionName: String,
  private val path: String,
) {
  private var sqlLiteral: String? = null
  private var requestName: String? = null
  private var responseName: String? = null

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

  inline fun <reified T> column(name: String, description: String = "") {
    declaredColumns[name] = SqlApiField(name, typeOf<T>().asTypeName(), description)
  }

  fun request(name: String) {
    requestName = name
  }

  fun response(name: String) {
    responseName = name
  }

  fun toSqlQueryToGenerateVo(): SqlQueryToGenerateVo {
    val sql = requireNotNull(sqlLiteral) { "SQL is required for query '$functionName'" }
    val metadata = SqlSelectParser.parse(sql)
    val defaultType = String::class.asTypeName().copy(nullable = true)
    val params = metadata.parameterNames.map { paramName ->
      declaredParams[paramName] ?: SqlApiField(paramName, defaultType)
    } + declaredParams.values.filterNot { declaredParam ->
      metadata.parameterNames.contains(declaredParam.name)
    }
    val columns = metadata.columnAliases.map { columnName ->
      declaredColumns[columnName] ?: SqlApiField(columnName, defaultType)
    } + declaredColumns.values.filterNot { declaredColumn ->
      metadata.columnAliases.contains(declaredColumn.name)
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
    )
  }
}
