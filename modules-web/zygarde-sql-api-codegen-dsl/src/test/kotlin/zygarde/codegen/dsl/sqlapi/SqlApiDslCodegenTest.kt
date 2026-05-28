package zygarde.codegen.dsl.sqlapi

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod

class SqlApiDslCodegenTest : SqlApiDslCodegen() {
  override fun codegen() {
    sqlApi("TodoReportApi", "/api/todo-report") {
      query("searchTodos", "/search") {
        sql(
          """
          select t.id as id, t.description as description, t.created_by as createdBy
          from todo t
          where (:keyword is null or t.description like concat('%', :keyword, '%'))
          """.trimIndent()
        )
        param<String?>("keyword", description = "Search keyword")
        column<Int>("id")
        column<String>("description")
        request("SearchTodosReq")
        response("TodoReportDto")
      }
    }
  }

  @Test
  fun `should collect SQL API metadata with nullable params and default columns`() {
    codegen()

    val query = apisToGenerate.single().queries.single()

    query.params.single().also {
      it.name shouldBe "keyword"
      it.type.isNullable shouldBe true
      it.description shouldBe "Search keyword"
    }
    query.columns.shouldHaveSize(3)
    query.columns[0].name shouldBe "id"
    query.columns[0].type.isNullable shouldBe false
    query.columns[1].name shouldBe "description"
    query.columns[1].type.isNullable shouldBe false
    query.columns[2].name shouldBe "createdBy"
    query.columns[2].type.isNullable shouldBe true
  }

  @Test
  fun `should collect API datasource qualifier and transaction policy`() {
    val codegen = object : SqlApiDslCodegen() {
      override fun codegen() {
        sqlApi("TodoReportApi", "/api/todo-report") {
          database {
            dataSource("reportingDataSource")
            transactionManager("reportingTransactionManager")
          }
          readOnlyTransactional()
          query("searchTodos", "/search") {
            sql("select id as id from todo")
            column<Int>("id")
          }
        }
      }
    }

    codegen.codegen()

    codegen.apisToGenerate.single().also {
      it.database.dataSourceQualifier shouldBe "reportingDataSource"
      it.database.transactionManagerQualifier shouldBe "reportingTransactionManager"
      it.transactionPolicy shouldBe SqlApiTransactionPolicy.READ_ONLY
    }
  }

  @Test
  fun `should collect function transaction override`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo where id = :id")
    query.param<Int>("id")
    query.column<Int>("id")
    query.transactional(readOnly = true)

    query.toSqlQueryToGenerateVo().transactionPolicy shouldBe SqlApiTransactionPolicy.READ_ONLY

    val command = DslSqlCommand("updateTodo", "/update")
    command.sql("update todo set description = :description where id = :id")
    command.param<Int>("id")
    command.param<String>("description")
    command.transactional(false)

    command.toSqlCommandToGenerateVo().transactionPolicy shouldBe SqlApiTransactionPolicy.NONE
  }

  @Test
  fun `should reject declared params that SQL does not use`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo where id = :id")
    query.param<Int>("id")
    query.param<String>("unused")
    query.column<Int>("id")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message shouldBe "SQL query 'searchTodos' declares parameters that are not used by SQL: unused"
  }

  @Test
  fun `should reject declared columns that SQL does not select`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo")
    query.column<Int>("missing")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message shouldBe "SQL query 'searchTodos' declares columns that are not selected by SQL: missing"
  }

  @Test
  fun `should collect page metadata and default page params`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql(
      """
      select id as id
      from todo
      where (:keyword is null or description like :keyword)
      order by id
      limit :pageSize offset :offset
      """.trimIndent()
    )
    query.returnsPage(
      countSql = """
        select count(*) as totalCount
        from todo
        where (:keyword is null or description like :keyword)
      """.trimIndent()
    )
    query.param<String?>("keyword")
    query.column<Int>("id")

    val metadata = query.toSqlQueryToGenerateVo()

    metadata.resultShape shouldBe SqlQueryResultShape.PAGE
    metadata.params.map { it.name } shouldBe listOf("keyword", "pageSize", "atPage")
    metadata.params.first { it.name == "pageSize" }.type.isNullable shouldBe false
    metadata.params.first { it.name == "atPage" }.type.isNullable shouldBe false
  }

  @Test
  fun `should collect query path and query parameter sources`() {
    val query = DslSqlQuery("findTodo", "/find/{id}")
    query.sql("select id as id from todo where id = :id and (:keyword is null or description like :keyword)")
    query.pathParam<Int>("id")
    query.queryParam<String?>("keyword")
    query.column<Int>("id")

    val metadata = query.toSqlQueryToGenerateVo()

    metadata.params.first { it.name == "id" }.source shouldBe SqlApiParamSource.PATH
    metadata.params.first { it.name == "keyword" }.source shouldBe SqlApiParamSource.QUERY
  }

  @Test
  fun `should reject query path parameter not present in path`() {
    val query = DslSqlQuery("findTodo", "/find")
    query.sql("select id as id from todo where id = :id")
    query.pathParam<Int>("id")
    query.column<Int>("id")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message shouldBe "SQL query 'findTodo' declares path parameters that are not present in path '/find': id"
  }

  @Test
  fun `should reject page SQL without offset parameter`() {
    val query = DslSqlQuery("searchTodos", "/search")
    query.sql("select id as id from todo limit :pageSize")
    query.returnsPage("select count(*) as totalCount from todo")

    val error = shouldThrow<IllegalArgumentException> {
      query.toSqlQueryToGenerateVo()
    }

    error.message.shouldContain("must use offset parameter")
  }

  @Test
  fun `should collect command metadata with generated key`() {
    val command = DslSqlCommand("createTodo", "/create")
    command.sql("insert into todo(description, check_times) values (:description, :checkTimes)")
    command.param<String>("description")
    command.param<Int>("checkTimes")
    command.returnsGeneratedKey<Int>("id", responseName = "CreatedTodoKeyDto")

    val metadata = command.toSqlCommandToGenerateVo()

    metadata.method shouldBe RequestMethod.POST
    metadata.resultShape shouldBe SqlCommandResultShape.GENERATED_KEY
    metadata.params.map { it.name } shouldBe listOf("description", "checkTimes")
    metadata.generatedKey?.responseName shouldBe "CreatedTodoKeyDto"
    metadata.generatedKey?.field?.name shouldBe "id"
  }

  @Test
  fun `should collect command path query and body parameter sources`() {
    val command = DslSqlCommand("updateTodo", "/update/{id}")
    command.put()
    command.sql("update todo set description = :description where id = :id and owner_id = :ownerId")
    command.pathParam<Int>("id")
    command.queryParam<Int>("ownerId")
    command.bodyParam<String>("description")

    val metadata = command.toSqlCommandToGenerateVo()

    metadata.params.first { it.name == "description" }.source shouldBe SqlApiParamSource.BODY
    metadata.params.first { it.name == "id" }.source shouldBe SqlApiParamSource.PATH
    metadata.params.first { it.name == "ownerId" }.source shouldBe SqlApiParamSource.QUERY
  }

  @Test
  fun `should reject command body parameter for delete`() {
    val command = DslSqlCommand("deleteTodo", "/delete/{id}")
    command.delete()
    command.sql("delete from todo where id = :id and reason = :reason")
    command.pathParam<Int>("id")
    command.bodyParam<String>("reason")

    val error = shouldThrow<IllegalArgumentException> {
      command.toSqlCommandToGenerateVo()
    }

    error.message shouldBe "SQL command 'deleteTodo' supports bodyParam only for POST, PUT, and PATCH"
  }

  @Test
  fun `should reject generated key for non insert command`() {
    val command = DslSqlCommand("updateTodo", "/update")
    command.sql("update todo set description = :description where id = :id")
    command.param<Int>("id")
    command.param<String>("description")
    command.returnsGeneratedKey<Int>("id")

    val error = shouldThrow<IllegalArgumentException> {
      command.toSqlCommandToGenerateVo()
    }

    error.message shouldBe "SQL command 'updateTodo' can return generated keys only for INSERT statements"
  }

  @Test
  fun `should reject command declared params that SQL does not use`() {
    val command = DslSqlCommand("deleteTodo", "/delete")
    command.sql("delete from todo where id = :id")
    command.param<Int>("id")
    command.param<String>("unused")

    val error = shouldThrow<IllegalArgumentException> {
      command.toSqlCommandToGenerateVo()
    }

    error.message shouldBe "SQL command 'deleteTodo' declares parameters that are not used by SQL: unused"
  }
}
