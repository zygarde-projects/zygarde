package zygarde.codegen.dsl.sqlapi

class DslSqlApi(
  private val config: SqlApiDslCodegenConfig,
  private val apiName: String,
  private val basePath: String,
) {
  private var sqlValidationRules: List<SqlValidationRule> = emptyList()
  private val queries: MutableList<SqlQueryToGenerateVo> = mutableListOf()
  private val commands: MutableList<SqlCommandToGenerateVo> = mutableListOf()
  private var database: SqlApiDatabaseToGenerateVo = SqlApiDatabaseToGenerateVo()
  private var transactionPolicy: SqlApiTransactionPolicy? = null

  internal constructor(
    config: SqlApiDslCodegenConfig,
    apiName: String,
    basePath: String,
    sqlValidationRules: List<SqlValidationRule>,
  ) : this(config, apiName, basePath) {
    this.sqlValidationRules = sqlValidationRules
  }

  fun database(dsl: DslSqlDatabase.() -> Unit) {
    database = DslSqlDatabase().also(dsl).toSqlApiDatabaseToGenerateVo()
  }

  fun transactional(readOnly: Boolean = false) {
    transactionPolicy = if (readOnly) {
      SqlApiTransactionPolicy.READ_ONLY
    } else {
      SqlApiTransactionPolicy.READ_WRITE
    }
  }

  fun readOnlyTransactional() {
    transactional(readOnly = true)
  }

  fun query(functionName: String, path: String, dsl: DslSqlQuery.() -> Unit) {
    queries.add(DslSqlQuery(functionName, path).also(dsl).toSqlQueryToGenerateVo(apiName, sqlValidationRules))
  }

  fun command(functionName: String, path: String, dsl: DslSqlCommand.() -> Unit) {
    commands.add(DslSqlCommand(functionName, path).also(dsl).toSqlCommandToGenerateVo(apiName, sqlValidationRules))
  }

  fun toSqlApiToGenerateVo(): SqlApiToGenerateVo {
    return SqlApiToGenerateVo(
      config = config,
      apiName = apiName,
      basePath = basePath,
      queries = queries,
      commands = commands,
      database = database,
      transactionPolicy = transactionPolicy,
    )
  }
}

class DslSqlDatabase {
  private var dataSourceQualifier: String? = null
  private var transactionManagerQualifier: String? = null

  fun dataSource(beanName: String) {
    dataSourceQualifier = beanName
  }

  fun transactionManager(beanName: String) {
    transactionManagerQualifier = beanName
  }

  fun toSqlApiDatabaseToGenerateVo(): SqlApiDatabaseToGenerateVo {
    return SqlApiDatabaseToGenerateVo(
      dataSourceQualifier = dataSourceQualifier,
      transactionManagerQualifier = transactionManagerQualifier,
    )
  }
}
