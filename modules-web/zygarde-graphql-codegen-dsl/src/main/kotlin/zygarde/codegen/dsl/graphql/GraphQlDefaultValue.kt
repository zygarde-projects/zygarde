package zygarde.codegen.dsl.graphql

import zygarde.codegen.model.graphql.requireGraphQlName

object GraphQlDefaultValue {
  fun string(value: String): String {
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

  fun int(value: Int): String = value.toString()

  fun long(value: Long): String = value.toString()

  fun float(value: Float): String {
    require(value.isFinite()) {
      "GraphQL Float default values must be finite"
    }
    return value.toString()
  }

  fun double(value: Double): String {
    require(value.isFinite()) {
      "GraphQL Float default values must be finite"
    }
    return value.toString()
  }

  fun boolean(value: Boolean): String = value.toString()

  fun enum(value: Enum<*>): String = enum(value.name)

  fun enum(name: String): String {
    requireGraphQlName(name, "GraphQL enum default value")
    return name
  }

  fun list(vararg values: String): String {
    return list(values.asIterable())
  }

  fun list(values: Iterable<String>): String {
    return values.joinToString(prefix = "[", postfix = "]")
  }

  fun objectValue(vararg fields: Pair<String, String>): String {
    return objectValue(fields.asIterable())
  }

  fun objectValue(fields: Iterable<Pair<String, String>>): String {
    val fieldList = fields.toList()
    fieldList.forEach { (name, _) -> requireGraphQlName(name, "GraphQL object default field") }
    return fieldList.joinToString(prefix = "{ ", postfix = " }") { (name, value) -> "$name: $value" }
      .takeUnless { fieldList.isEmpty() }
      ?: "{}"
  }

  fun nullValue(): String = "null"
}
