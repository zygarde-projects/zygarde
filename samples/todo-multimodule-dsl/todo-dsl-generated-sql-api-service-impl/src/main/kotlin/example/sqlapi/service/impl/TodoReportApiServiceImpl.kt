package example.sqlapi.service.`impl`

import example.CurrentTodoIdResolver
import example.FileDtoProvider
import example.sqlapi.dto.TodoReportDto
import example.sqlapi.service.TodoReportApiService
import javax.sql.DataSource
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.beans.factory.`annotation`.Qualifier
import org.springframework.stereotype.Service
import zygarde.`data`.api.PageDto
import zygarde.core.di.DiServiceContext.bean
import zygarde.sql.api.ZygardeSqlExecutor

@Service
public class TodoReportApiServiceImpl(
  @Autowired
  @Qualifier("dataSource")
  private val dataSource: DataSource,
  @Autowired
  private val fileDtoProvider: FileDtoProvider,
) : TodoReportApiService {
  private val executor: ZygardeSqlExecutor = ZygardeSqlExecutor(dataSource)

  override fun searchTodos(keyword: String?): Collection<TodoReportDto> {
    val params = mapOf(
      "keyword" to keyword,
    )
    val rows = executor.query(SEARCH_TODOS_SQL, params) { row ->
      SearchTodosRow(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description"),
        fileId = row.getNullable<String>("fileId")
      )
    }
    val fileKeys = rows.mapNotNull { it.fileId }.distinct()
    val fileValues = fileDtoProvider.load(fileKeys)
    val items = rows.map { row ->
      TodoReportDto(
        id = row.id,
        description = row.description,
        `file` = row.fileId?.let { fileValues[it] }
      )
    }
    return items
  }

  override fun findTodo(id: Int): TodoReportDto? {
    val params = mapOf(
      "id" to id,
    )
    val rows = executor.query(FIND_TODO_SQL, params) { row ->
      FindTodoRow(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description"),
        fileId = row.getNullable<String>("fileId")
      )
    }
    val fileKeys = rows.mapNotNull { it.fileId }.distinct()
    val fileValues = fileDtoProvider.load(fileKeys)
    val items = rows.map { row ->
      TodoReportDto(
        id = row.id,
        description = row.description,
        `file` = row.fileId?.let { fileValues[it] }
      )
    }
    return items.singleOrNull()
  }

  override fun findTodosByIds(ids: Collection<Int>): Collection<TodoReportDto> {
    val params = mapOf(
      "ids" to ids,
    )
    val rows = executor.query(FIND_TODOS_BY_IDS_SQL, params) { row ->
      FindTodosByIdsRow(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description"),
        fileId = row.getNullable<String>("fileId")
      )
    }
    val fileKeys = rows.mapNotNull { it.fileId }.distinct()
    val fileValues = fileDtoProvider.load(fileKeys)
    val items = rows.map { row ->
      TodoReportDto(
        id = row.id,
        description = row.description,
        `file` = row.fileId?.let { fileValues[it] }
      )
    }
    return items
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
    val rows = executor.query(PAGE_TODOS_SQL, params) { row ->
      PageTodosRow(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description"),
        fileId = row.getNullable<String>("fileId")
      )
    }
    val fileKeys = rows.mapNotNull { it.fileId }.distinct()
    val fileValues = fileDtoProvider.load(fileKeys)
    val items = rows.map { row ->
      TodoReportDto(
        id = row.id,
        description = row.description,
        `file` = row.fileId?.let { fileValues[it] }
      )
    }
    val totalCount = executor.queryOne(PAGE_TODOS_COUNT_SQL, params) { row ->
      row.getRequired<Long>("totalCount")
    }
    val totalPages = if (pageSize <= 0) 0 else ((totalCount + pageSize - 1) / pageSize).toInt()
    return PageDto(atPage, totalPages, items, totalCount)
  }

  override fun findCurrentTodo(): TodoReportDto? {
    val params = mapOf(
      "currentTodoId" to bean<CurrentTodoIdResolver>().resolve("currentTodoId"),
    )
    val rows = executor.query(FIND_CURRENT_TODO_SQL, params) { row ->
      FindCurrentTodoRow(
        id = row.getRequired<Int>("id"),
        description = row.getRequired<String>("description"),
        fileId = row.getNullable<String>("fileId")
      )
    }
    val fileKeys = rows.mapNotNull { it.fileId }.distinct()
    val fileValues = fileDtoProvider.load(fileKeys)
    val items = rows.map { row ->
      TodoReportDto(
        id = row.id,
        description = row.description,
        `file` = row.fileId?.let { fileValues[it] }
      )
    }
    return items.singleOrNull()
  }

  private data class SearchTodosRow(
    public val id: Int,
    public val description: String,
    public val fileId: String?,
  )

  private data class FindTodoRow(
    public val id: Int,
    public val description: String,
    public val fileId: String?,
  )

  private data class FindTodosByIdsRow(
    public val id: Int,
    public val description: String,
    public val fileId: String?,
  )

  private data class PageTodosRow(
    public val id: Int,
    public val description: String,
    public val fileId: String?,
  )

  private data class FindCurrentTodoRow(
    public val id: Int,
    public val description: String,
    public val fileId: String?,
  )

  public companion object {
    private const val SEARCH_TODOS_SQL: String =
        "select t.id as id, t.description as description, t.file_id as fileId\nfrom todo t\nwhere (:keyword is null or t.description like concat('%', :keyword, '%'))\norder by t.id"

    private const val FIND_TODO_SQL: String =
        "select t.id as id, t.description as description, t.file_id as fileId\nfrom todo t\nwhere t.id = :id"

    private const val FIND_TODOS_BY_IDS_SQL: String =
        "select t.id as id, t.description as description, t.file_id as fileId\nfrom todo t\nwhere t.id in (:ids)\norder by t.id"

    private const val PAGE_TODOS_SQL: String =
        "select t.id as id, t.description as description, t.file_id as fileId\nfrom todo t\nwhere (:keyword is null or t.description like concat('%', :keyword, '%'))\norder by t.id\nlimit :pageSize offset :offset"

    private const val PAGE_TODOS_COUNT_SQL: String =
        "select count(*) as totalCount\nfrom todo t\nwhere (:keyword is null or t.description like concat('%', :keyword, '%'))"

    private const val FIND_CURRENT_TODO_SQL: String =
        "select t.id as id, t.description as description, t.file_id as fileId\nfrom todo t\nwhere t.id = :currentTodoId"
  }
}
