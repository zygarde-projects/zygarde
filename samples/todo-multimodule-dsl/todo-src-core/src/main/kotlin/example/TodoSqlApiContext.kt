package example

import org.springframework.stereotype.Component
import zygarde.sql.api.SqlApiContextParamResolver

@Component
class TodoSqlApiContext {
  var currentTodoId: Int? = null
}

@Component
class CurrentTodoIdResolver(
  private val context: TodoSqlApiContext,
) : SqlApiContextParamResolver<Int?> {
  override fun resolve(paramName: String): Int? = context.currentTodoId
}
