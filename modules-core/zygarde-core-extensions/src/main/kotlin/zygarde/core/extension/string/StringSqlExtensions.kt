package zygarde.core.extension.string

import java.util.concurrent.atomic.AtomicInteger

object StringSqlExtensions {

  private val PATTERN_SQL_SELECT = "select (.*) from ".toRegex(RegexOption.IGNORE_CASE).toPattern()

  fun String.shortenSelectSql(removeColAlias: Boolean = true): String {
    var resultSql = this.replace("\\n|\\r\\n".toRegex(), " ")
      .replace(" +".toRegex(), " ")
      .trim()
    val matcher = PATTERN_SQL_SELECT.matcher(resultSql)
    val resolvedFields = mutableSetOf<String>()
    if (matcher.find()) {
      val tableCounter = AtomicInteger(1)
      val resolvedTables = mutableSetOf<String>()
      val matched = matcher.group(1)
        .replace("select ".toRegex(RegexOption.IGNORE_CASE), "")
        .replace(" from".toRegex(RegexOption.IGNORE_CASE), " ")
      for (s in matched.split(",")) {
        if (s.contains(" as ".toRegex(RegexOption.IGNORE_CASE))) {
          val splitByAs = s.split(" as ".toRegex(RegexOption.IGNORE_CASE))
          val tableAndCol = splitByAs[0]
          var hasSameCol = false
          if (tableAndCol.contains(".")) {
            val tableAndColSplitByDot = tableAndCol.split(".")
            val table = tableAndColSplitByDot[0].replace(" ".toRegex(), "")
            resolvedTables.add(table)

            val field = tableAndColSplitByDot[1].replace(" ".toRegex(), "")
            hasSameCol = resolvedFields.add(field)
          }
          val fieldAlias = splitByAs[1].trim()
          if (hasSameCol || removeColAlias) {
            resultSql = resultSql.replace(" as $fieldAlias".toRegex(RegexOption.IGNORE_CASE), "")
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
