package zygarde.sql.api

fun interface SqlApiContextParamResolver<T> {
  fun resolve(paramName: String): T
}
