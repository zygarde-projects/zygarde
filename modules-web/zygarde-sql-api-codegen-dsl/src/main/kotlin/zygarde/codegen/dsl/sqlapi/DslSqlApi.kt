package zygarde.codegen.dsl.sqlapi

class DslSqlApi(
  private val config: SqlApiDslCodegenConfig,
  private val apiName: String,
  private val basePath: String,
) {
  private val queries: MutableList<SqlQueryToGenerateVo> = mutableListOf()

  fun query(functionName: String, path: String, dsl: DslSqlQuery.() -> Unit) {
    queries.add(DslSqlQuery(functionName, path).also(dsl).toSqlQueryToGenerateVo())
  }

  fun toSqlApiToGenerateVo(): SqlApiToGenerateVo {
    return SqlApiToGenerateVo(
      config = config,
      apiName = apiName,
      basePath = basePath,
      queries = queries,
    )
  }
}
