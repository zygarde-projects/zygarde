package zygarde.codegen.dsl.sqlapi

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.update.Update
import zygarde.sql.api.NamedParameterSql

data class SqlCommandMetadata(
  val parameterNames: List<String>,
  val kind: SqlCommandKind,
)

enum class SqlCommandKind {
  INSERT,
  UPDATE,
  DELETE,
}

object SqlCommandParser {
  fun parse(sql: String): SqlCommandMetadata {
    val parsedSql = NamedParameterSql.parse(sql)
    val kind = parseKind(parsedSql.sql)

    return SqlCommandMetadata(
      parameterNames = parsedSql.parameterNames.distinct(),
      kind = kind,
    )
  }

  private fun parseKind(sql: String): SqlCommandKind {
    return try {
      when (CCJSqlParserUtil.parse(sql)) {
        is Insert -> SqlCommandKind.INSERT
        is Update -> SqlCommandKind.UPDATE
        is Delete -> SqlCommandKind.DELETE
        else -> throw IllegalArgumentException("SQL API command only supports INSERT, UPDATE, and DELETE statements")
      }
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: Exception) {
      throw IllegalArgumentException("Failed to parse SQL command: ${e.message}", e)
    }
  }
}
