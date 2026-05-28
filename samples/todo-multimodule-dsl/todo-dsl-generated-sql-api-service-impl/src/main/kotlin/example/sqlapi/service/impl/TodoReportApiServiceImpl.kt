package example.sqlapi.service.`impl`

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

  override fun searchTodos(keyword: String?): Collection<TodoReportDto> {
    val params = mapOf(
      "keyword" to keyword,
    )
    return executor.query(SEARCH_TODOS_SQL, params) { row ->
      TodoReportDto(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description")
      )
    }
  }

  override fun findTodo(id: Int): TodoReportDto? {
    val params = mapOf(
      "id" to id,
    )
    return executor.queryOneOrNull(FIND_TODO_SQL, params) { row ->
      TodoReportDto(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description")
      )
    }
  }

  override fun pageTodos(
    keyword: String?,
    pageSize: Int,
    atPage: Int,
  ): PageDto<TodoReportDto> {
    val params = mapOf(
      "keyword" to keyword,
      "pageSize" to pageSize,
      "atPage" to atPage,
      "offset" to (atPage * pageSize),
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
    val totalPages = if (pageSize <= 0) 0 else ((totalCount + pageSize - 1) / pageSize).toInt()
    return PageDto(atPage, totalPages, items, totalCount)
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
