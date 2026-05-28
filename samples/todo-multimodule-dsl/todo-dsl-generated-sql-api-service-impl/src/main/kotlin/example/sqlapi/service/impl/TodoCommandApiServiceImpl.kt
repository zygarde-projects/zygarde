package example.sqlapi.service.`impl`

import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.UpdateTodoBySqlReq
import example.sqlapi.service.TodoCommandApiService
import javax.sql.DataSource
import kotlin.Int
import kotlin.String
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.beans.factory.`annotation`.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.`annotation`.Transactional
import zygarde.sql.api.ZygardeSqlExecutor

@Service
public class TodoCommandApiServiceImpl(
  @Autowired
  @Qualifier("dataSource")
  private val dataSource: DataSource,
) : TodoCommandApiService {
  private val executor: ZygardeSqlExecutor = ZygardeSqlExecutor(dataSource)

  @Transactional(transactionManager = "transactionManager")
  override fun createTodo(req: CreateTodoBySqlReq): CreatedTodoKeyDto {
    val params = mapOf(
      "description" to req.description,
    )
    val key = executor.insertAndReturnKey<Int>(CREATE_TODO_SQL, params, "id")
    return CreatedTodoKeyDto(id = key)
  }

  @Transactional(transactionManager = "transactionManager")
  override fun updateTodo(id: Int, req: UpdateTodoBySqlReq): Int {
    val params = mapOf(
      "description" to req.description,
      "id" to id,
    )
    return executor.execute(UPDATE_TODO_SQL, params)
  }

  @Transactional(transactionManager = "transactionManager")
  override fun deleteTodo(id: Int) {
    val params = mapOf(
      "id" to id,
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
