package example.sqlapi.service.`impl`

import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.DeleteTodoBySqlReq
import example.sqlapi.dto.UpdateTodoBySqlReq
import example.sqlapi.service.TodoCommandApiService
import javax.sql.DataSource
import kotlin.Int
import kotlin.String
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Service
import zygarde.sql.api.ZygardeSqlExecutor

@Service
public class TodoCommandApiServiceImpl(
  @Autowired
  private val dataSource: DataSource,
) : TodoCommandApiService {
  private val executor: ZygardeSqlExecutor = ZygardeSqlExecutor(dataSource)

  override fun createTodo(req: CreateTodoBySqlReq): CreatedTodoKeyDto {
    val params = mapOf(
      "description" to req.description,
    )
    val key = executor.insertAndReturnKey<Int>(CREATE_TODO_SQL, params, "id")
    return CreatedTodoKeyDto(id = key)
  }

  override fun updateTodo(req: UpdateTodoBySqlReq): Int {
    val params = mapOf(
      "description" to req.description,
      "id" to req.id,
    )
    return executor.execute(UPDATE_TODO_SQL, params)
  }

  override fun deleteTodo(req: DeleteTodoBySqlReq) {
    val params = mapOf(
      "id" to req.id,
    )
    executor.execute(DELETE_TODO_SQL, params)
  }

  public companion object {
    private const val CREATE_TODO_SQL: String =
        "insert into todo(description, check_times)\nvalues (:description, 0)"

    private const val UPDATE_TODO_SQL: String =
        "update todo\nset description = :description\nwhere id = :id"

    private const val DELETE_TODO_SQL: String = "delete from todo\nwhere id = :id"
  }
}
