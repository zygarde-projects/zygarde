package zygarde.sql.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Test

class ZygardeSqlExecutorTest {
  private fun dataSource(): JdbcDataSource {
    return JdbcDataSource().also {
      it.setURL("jdbc:h2:mem:${System.nanoTime()};MODE=MYSQL;DB_CLOSE_DELAY=-1")
    }
  }

  @Test
  fun `should execute query with nullable values and typed conversion`() {
    val dataSource = dataSource()
    dataSource.connection.use { connection ->
      connection.createStatement().use { statement ->
        statement.execute("create table todo(id int primary key, description varchar(255), done boolean)")
        statement.execute("insert into todo(id, description, done) values (1, 'first', true), (2, null, false)")
      }
    }

    val rows = ZygardeSqlExecutor(dataSource).query(
      "select id as id, description as description, done as done from todo where (:keyword is null or description like :keyword) order by id",
      mapOf("keyword" to null)
    ) { row ->
      Triple(
        row.getRequired<Int>("id"),
        row.getNullable<String>("description"),
        row.getRequired<Boolean>("done")
      )
    }

    rows.shouldHaveSize(2)
    rows[0] shouldBe Triple(1, "first", true)
    rows[1] shouldBe Triple(2, null, false)
  }

  @Test
  fun `should fail when required column is missing or null`() {
    val missingColumnError = shouldThrow<IllegalArgumentException> {
      ZygardeSqlRow(mapOf("id" to 1)).getRequired<String>("description")
    }
    missingColumnError.message shouldBe "SQL column 'description' was not found"

    val nullColumnError = shouldThrow<IllegalStateException> {
      ZygardeSqlRow(mapOf("description" to null)).getRequired<String>("description")
    }
    nullColumnError.message shouldBe "Required SQL column 'description' was null"
  }
}
