package zygarde.core.extension.string

import java.util.concurrent.atomic.AtomicInteger

object StringSqlExtensions {

  private val PATTERN_SQL_SELECT = "select (.*) from ".toRegex(RegexOption.IGNORE_CASE).toPattern()
  private val PATTERN_SQL_TABLE_AND_COL = "\\(?(\\w+)\\.(\\w+)\\)? as (\\w+)".toRegex(RegexOption.IGNORE_CASE).toPattern()

  fun String.shortenSelectSql(removeColAlias: Boolean = true): String {
    var resultSql = this.replace("\\n|\\r\\n".toRegex(), " ")
      .replace(" +".toRegex(), " ")
      .trim()
    val matcher = PATTERN_SQL_SELECT.matcher(resultSql)
    val resolvedCols = mutableSetOf<String>()
    if (matcher.find()) {
      val tableCounter = AtomicInteger(1)
      val resolvedTables = mutableSetOf<String>()
      val matched = matcher.group(1)
        .replace("select ".toRegex(RegexOption.IGNORE_CASE), "")
        .replace(" from".toRegex(RegexOption.IGNORE_CASE), " ")

      for (s in matched.split(",")) {
        if (s.contains(" as ".toRegex(RegexOption.IGNORE_CASE))) {
          val tableAndColMatcher = PATTERN_SQL_TABLE_AND_COL.matcher(s)
          if (tableAndColMatcher.find()) {
            val table = tableAndColMatcher.group(1)
            val col = tableAndColMatcher.group(2)
            val colAlias = tableAndColMatcher.group(3)
            resolvedTables.add(table)
            if (resolvedCols.add(col) || removeColAlias) {
              resultSql = resultSql.replace(" as $colAlias".toRegex(RegexOption.IGNORE_CASE), "")
            }
          }
        }
      }

      for (resolvedTable in resolvedTables) {
        resultSql = resultSql.replace(resolvedTable.toRegex(), "t${tableCounter.getAndIncrement()}")
      }
    }
    return resultSql
  }
}
