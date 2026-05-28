package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.TypeName
import org.springframework.web.bind.annotation.RequestMethod

data class SqlApiToGenerateVo(
  val config: SqlApiDslCodegenConfig,
  val apiName: String,
  val basePath: String,
  val queries: List<SqlQueryToGenerateVo>,
  val commands: List<SqlCommandToGenerateVo> = emptyList(),
  val database: SqlApiDatabaseToGenerateVo = SqlApiDatabaseToGenerateVo(),
  val transactionPolicy: SqlApiTransactionPolicy? = null,
)

data class SqlApiDatabaseToGenerateVo(
  val dataSourceQualifier: String? = null,
  val transactionManagerQualifier: String? = null,
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
  val transactionPolicy: SqlApiTransactionPolicy? = null,
)

data class SqlApiField(
  val name: String,
  val type: TypeName,
  val description: String = "",
  val source: SqlApiParamSource = SqlApiParamSource.AUTO,
  val contextValueSource: SqlApiContextValueSource? = null,
)

enum class SqlApiParamSource {
  AUTO,
  PATH,
  QUERY,
  BODY,
  CONTEXT,
}

sealed class SqlApiContextValueSource {
  data class ResolverByType(
    val resolverType: TypeName,
  ) : SqlApiContextValueSource()

  data class ResolverByName(
    val beanName: String,
  ) : SqlApiContextValueSource()
}

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

data class SqlCommandToGenerateVo(
  val functionName: String,
  val path: String,
  val sql: String,
  val method: RequestMethod,
  val requestName: String,
  val params: List<SqlApiField>,
  val resultShape: SqlCommandResultShape = SqlCommandResultShape.AFFECTED_ROWS,
  val generatedKey: SqlGeneratedKeyToGenerateVo? = null,
  val transactionPolicy: SqlApiTransactionPolicy? = null,
)

enum class SqlCommandResultShape {
  AFFECTED_ROWS,
  NO_CONTENT,
  GENERATED_KEY,
}

data class SqlGeneratedKeyToGenerateVo(
  val responseName: String,
  val field: SqlApiField,
  val keyColumnName: String,
)

enum class SqlApiTransactionPolicy {
  NONE,
  READ_ONLY,
  READ_WRITE,
}
