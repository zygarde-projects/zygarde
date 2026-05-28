package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.TypeName

data class SqlApiToGenerateVo(
  val config: SqlApiDslCodegenConfig,
  val apiName: String,
  val basePath: String,
  val queries: List<SqlQueryToGenerateVo>,
)

data class SqlQueryToGenerateVo(
  val functionName: String,
  val path: String,
  val sql: String,
  val requestName: String,
  val responseName: String,
  val params: List<SqlApiField>,
  val columns: List<SqlApiField>,
  val resultShape: SqlQueryResultShape = SqlQueryResultShape.LIST,
  val page: SqlApiPageToGenerateVo? = null,
)

data class SqlApiField(
  val name: String,
  val type: TypeName,
  val description: String = "",
)

enum class SqlQueryResultShape {
  LIST,
  ONE,
  ONE_NULLABLE,
  PAGE,
}

data class SqlApiPageToGenerateVo(
  val countSql: String,
  val countColumnName: String,
  val pageParamName: String,
  val pageSizeParamName: String,
  val offsetParamName: String,
)
