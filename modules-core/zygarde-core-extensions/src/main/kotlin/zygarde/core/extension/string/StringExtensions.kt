package zygarde.core.extension.string

import java.util.regex.Pattern

private val PATTERN_NUMERIC: Pattern = Pattern.compile("-?\\d+(\\.\\d+)?")

fun String.replaceByArgs(vararg args: Any?) = args
  .fold(this) { s, arg -> s.replaceFirst("\\{}".toRegex(), arg.toString()) }

fun String.isNumeric(): Boolean = PATTERN_NUMERIC.matcher(this).matches()
