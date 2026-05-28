package zygarde.sql.api

data class ParsedSql(
  val sql: String,
  val parameterNames: List<String>,
)

object NamedParameterSql {
  fun parse(sql: String): ParsedSql {
    val parsedSql = StringBuilder(sql.length)
    val parameterNames = mutableListOf<String>()
    var index = 0
    var inSingleQuote = false
    var inDoubleQuote = false
    var inBacktickQuote = false
    var inBracketQuote = false
    var inLineComment = false
    var inBlockComment = false

    while (index < sql.length) {
      val char = sql[index]
      val next = sql.getOrNull(index + 1)

      if (inLineComment) {
        parsedSql.append(char)
        if (char == '\n' || char == '\r') {
          inLineComment = false
        }
        index++
        continue
      }

      if (inBlockComment) {
        parsedSql.append(char)
        if (char == '*' && next == '/') {
          parsedSql.append(next)
          inBlockComment = false
          index += 2
        } else {
          index++
        }
        continue
      }

      if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote && !inBracketQuote && char == '-' && next == '-') {
        parsedSql.append("--")
        inLineComment = true
        index += 2
        continue
      }

      if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote && !inBracketQuote && char == '/' && next == '*') {
        parsedSql.append("/*")
        inBlockComment = true
        index += 2
        continue
      }

      if (char == '\'') {
        parsedSql.append(char)
        if (!inDoubleQuote && !inBacktickQuote && !inBracketQuote && inSingleQuote && next == '\'') {
          parsedSql.append(next)
          index += 2
          continue
        }
        if (!inDoubleQuote && !inBacktickQuote && !inBracketQuote) {
          inSingleQuote = !inSingleQuote
        }
        index++
        continue
      }

      if (!inSingleQuote && !inBacktickQuote && !inBracketQuote && char == '"') {
        parsedSql.append(char)
        if (inDoubleQuote && next == '"') {
          parsedSql.append(next)
          index += 2
          continue
        }
        inDoubleQuote = !inDoubleQuote
        index++
        continue
      }

      if (!inSingleQuote && !inDoubleQuote && !inBracketQuote && char == '`') {
        parsedSql.append(char)
        if (inBacktickQuote && next == '`') {
          parsedSql.append(next)
          index += 2
          continue
        }
        inBacktickQuote = !inBacktickQuote
        index++
        continue
      }

      if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote && char == '[') {
        parsedSql.append(char)
        inBracketQuote = true
        index++
        continue
      }

      if (inBracketQuote && char == ']') {
        parsedSql.append(char)
        inBracketQuote = false
        index++
        continue
      }

      if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote && !inBracketQuote && char == ':' && next == ':') {
        parsedSql.append("::")
        index += 2
        continue
      }

      if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote && !inBracketQuote && char == ':' && next?.isSqlIdentifierStart() == true) {
        val nameStart = index + 1
        var nameEnd = nameStart + 1
        while (nameEnd < sql.length && sql[nameEnd].isSqlIdentifierPart()) {
          nameEnd++
        }
        parameterNames.add(sql.substring(nameStart, nameEnd))
        parsedSql.append('?')
        index = nameEnd
        continue
      }

      parsedSql.append(char)
      index++
    }

    return ParsedSql(parsedSql.toString(), parameterNames)
  }

  private fun Char.isSqlIdentifierStart(): Boolean = this == '_' || isLetter()

  private fun Char.isSqlIdentifierPart(): Boolean = this == '_' || isLetterOrDigit()
}
