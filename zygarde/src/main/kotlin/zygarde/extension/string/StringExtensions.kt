package zygarde.extension.string

fun String.replaceByArgs(vararg args: Any?) = args
  .fold(this, { s, arg -> s.replaceFirst("\\{}".toRegex(), arg.toString()) })
