package example.sqlapi.service.`impl`

import example.sqlapi.dto.FindTodoReq
import example.sqlapi.dto.PageTodosReq
import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.TodoReportDto
import example.sqlapi.service.TodoReportApiService
import javax.sql.DataSource
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Service
import zygarde.`data`.api.PageDto
import zygarde.sql.api.ZygardeSqlExecutor

@Service
public class TodoReportApiServiceImpl(
  @Autowired
  private val dataSource: DataSource,
) : TodoReportApiService {
  private val executor: ZygardeSqlExecutor = ZygardeSqlExecutor(dataSource)

  override fun searchTodos(req: SearchTodosReq): Collection<TodoReportDto> {
    val params = mapOf(
      "keyword" to req.keyword,
    )
    return executor.query(SEARCH_TODOS_SQL, params) { row ->
      TodoReportDto(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description")
      )
    }
  }

  override fun findTodo(req: FindTodoReq): TodoReportDto? {
    val params = mapOf(
      "id" to req.id,
    )
    return executor.queryOneOrNull(FIND_TODO_SQL, params) { row ->
      TodoReportDto(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description")
      )
    }
  }

  override fun pageTodos(req: PageTodosReq): PageDto<TodoReportDto> {
    val params = mapOf(
      "keyword" to req.keyword,
      "pageSize" to req.pageSize,
      "atPage" to req.atPage,
      "offset" to (req.atPage * req.pageSize),
    )
    val items = executor.query(PAGE_TODOS_SQL, params) { row ->
      TodoReportDto(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description")
      )
    }
    val totalCount = executor.queryOne(PAGE_TODOS_COUNT_SQL, params) { row ->
      row.getRequired<Long>("totalCount")
    }
    val totalPages = if (req.pageSize <= 0) 0 else ((totalCount + req.pageSize - 1) /
        req.pageSize).toInt()
    return PageDto(req.atPage, totalPages, items, totalCount)
  }

  public companion object {
    private const val SEARCH_TODOS_SQL: String =
        "select t.id as id, t.description as description\nfrom todo t\nwhere (:keyword is null or t.description like concat('%', :keyword, '%'))\norder by t.id"

    private const val FIND_TODO_SQL: String =
        "select t.id as id, t.description as description\nfrom todo t\nwhere t.id = :id"

    private const val PAGE_TODOS_SQL: String =
        "select t.id as id, t.description as description\nfrom todo t\nwhere (:keyword is null or t.description like concat('%', :keyword, '%'))\norder by t.id\nlimit :pageSize offset :offset"

    private const val PAGE_TODOS_COUNT_SQL: String =
        "select count(*) as totalCount\nfrom todo t\nwhere (:keyword is null or t.description like concat('%', :keyword, '%'))"
  }
}
