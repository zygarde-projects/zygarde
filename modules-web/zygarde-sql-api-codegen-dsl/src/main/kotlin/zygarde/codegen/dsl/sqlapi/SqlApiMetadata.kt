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
)

data class SqlApiField(
  val name: String,
  val type: TypeName,
  val description: String = "",
)
