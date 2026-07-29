package zygarde.codegen.dsl.sqlapi

import net.sf.jsqlparser.statement.Statement

/**
 * Inspects a successfully parsed SQL statement during SQL API code generation.
 *
 * [sql] is the original SQL supplied to the DSL. [statement] is the mutable JSqlParser 5.0 AST produced by the
 * codegen parser and must be treated as read-only. Rules run synchronously in registration order and share the same
 * statement instance. Return an empty list when the statement is valid.
 */
fun interface SqlValidationRule {
  fun validate(sql: String, statement: Statement): List<String>
}

internal data class ParsedSql<M>(
  val metadata: M,
  val statement: Statement,
)

internal data class SqlStatementToValidate(
  val role: String,
  val sql: String,
  val statement: Statement,
)

internal fun validateSqlStatements(
  apiName: String,
  declarationKind: String,
  functionName: String,
  rules: List<SqlValidationRule>,
  statements: List<SqlStatementToValidate>,
) {
  val findings = statements.flatMap { statement ->
    rules.flatMapIndexed { index, rule ->
      try {
        rule.validate(statement.sql, statement.statement).map { finding -> "${statement.role}: $finding" }
      } catch (e: Exception) {
        throw IllegalStateException(
          "SQL validation rule #${index + 1} threw while validating ${statement.role} for " +
            "SQL API '$apiName', $declarationKind '$functionName'",
          e,
        )
      }
    }
  }
  require(findings.isEmpty()) {
    "SQL validation failed for SQL API '$apiName', $declarationKind '$functionName':\n" +
      findings.joinToString("\n") { "- $it" }
  }
}
