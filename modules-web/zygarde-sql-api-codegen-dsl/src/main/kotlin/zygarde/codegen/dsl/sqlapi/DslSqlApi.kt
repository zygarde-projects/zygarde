package zygarde.codegen.dsl.sqlapi

class DslSqlApi(
  private val config: SqlApiDslCodegenConfig,
  private val apiName: String,
  private val basePath: String,
) {
  private val queries: MutableList<SqlQueryToGenerateVo> = mutableListOf()
  private val commands: MutableList<SqlCommandToGenerateVo> = mutableListOf()

  fun query(functionName: String, path: String, dsl: DslSqlQuery.() -> Unit) {
    queries.add(DslSqlQuery(functionName, path).also(dsl).toSqlQueryToGenerateVo())
  }

  fun command(functionName: String, path: String, dsl: DslSqlCommand.() -> Unit) {
    commands.add(DslSqlCommand(functionName, path).also(dsl).toSqlCommandToGenerateVo())
  }

  fun toSqlApiToGenerateVo(): SqlApiToGenerateVo {
    return SqlApiToGenerateVo(
      config = config,
      apiName = apiName,
      basePath = basePath,
      queries = queries,
      commands = commands,
    )
  }
}
