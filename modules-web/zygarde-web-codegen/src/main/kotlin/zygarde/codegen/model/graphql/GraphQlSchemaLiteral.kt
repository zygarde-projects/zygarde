package zygarde.codegen.model.graphql

/**
 * Renders [value] as a single-line GraphQL string literal (`"..."`), escaping quotes,
 * backslashes and control characters so the result is safe to embed directly in SDL.
 */
fun graphQlStringLiteral(value: String): String {
  return buildString {
    append('"')
    value.forEach { char ->
      append(
        when (char) {
          '"' -> "\\\""
          '\\' -> "\\\\"
          '\b' -> "\\b"
          '\u000c' -> "\\f"
          '\n' -> "\\n"
          '\r' -> "\\r"
          '\t' -> "\\t"
          else -> if (char.code < 0x20) {
            "\\u${char.code.toString(16).uppercase().padStart(4, '0')}"
          } else {
            char.toString()
          }
        }
      )
    }
    append('"')
  }
}
