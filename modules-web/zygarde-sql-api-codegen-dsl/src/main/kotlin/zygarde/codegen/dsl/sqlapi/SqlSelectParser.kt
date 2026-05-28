package zygarde.codegen.dsl.sqlapi

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.PlainSelect
import zygarde.sql.api.NamedParameterSql

data class SqlSelectMetadata(
  val parameterNames: List<String>,
  val columnAliases: List<String>,
)

object SqlSelectParser {
  private val splitByAsRegex = Regex("""(?i)\s+AS\s+""")
  private val functionCallRegex = Regex("""(?s).*\(.*\).*""")

  fun parse(sql: String): SqlSelectMetadata {
    val parameterNames = NamedParameterSql.parse(sql).parameterNames.distinct()
    val columnAliases = parseOutputColumns(sql)

    return SqlSelectMetadata(
      parameterNames = parameterNames,
      columnAliases = columnAliases,
    )
  }

  private fun parseOutputColumns(sql: String): List<String> {
    return try {
      val statement = CCJSqlParserUtil.parse(normalizeSql(sql))
      if (statement !is PlainSelect) {
        throw IllegalArgumentException("SQL API only supports SELECT statements")
      }

      var autoNameIndex = 1
      statement.selectItems.map { selectItem ->
        val selectField = selectItem.toString()
        if (selectField == "*" || selectField.endsWith(".*")) {
          throw IllegalArgumentException("SELECT * is not allowed. Please specify output columns.")
        }

        val splitByAs = selectField.split(splitByAsRegex, limit = 2)
        val source = splitByAs[0].trim()
        var name = if (splitByAs.size > 1) splitByAs[1].trim() else source

        if (splitByAs.size == 1 && name.matches(functionCallRegex)) {
          name = "output${autoNameIndex++}"
        }

        if (name.contains(".")) {
          name = name.substringAfterLast(".")
        }

        name.unquoteIdentifier()
      }
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: Exception) {
      throw IllegalArgumentException("Failed to parse SQL output columns: ${e.message}", e)
    }
  }

  private fun normalizeSql(sql: String): String {
    return sql.replace(Regex("""\[(.*?)]\s*\.""")) { "" }
  }

  private fun String.unquoteIdentifier(): String {
    return trim()
      .removeSurrounding("\"")
      .removeSurrounding("`")
      .removeSurrounding("[", "]")
  }
}
