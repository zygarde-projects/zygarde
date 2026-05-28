package zygarde.codegen.dsl.sqlapi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SqlSelectParserTest {
  @Test
  fun `should parse select params and aliases`() {
    val metadata = SqlSelectParser.parse(
      """
      select t.id as id, concat(':', t.description) as description
      from todo t
      where (:keyword is null or t.description like :keyword) and t.note <> ':ignored'
      """.trimIndent()
    )

    metadata.parameterNames.shouldContainExactly("keyword")
    metadata.columnAliases.shouldContainExactly("id", "description")
  }

  @Test
  fun `should parse CTE output columns`() {
    val metadata = SqlSelectParser.parse(
      """
      with latest_todo as (
        select id, description
        from todo
        where created_at >= :createdAt
      )
      select latest_todo.id, latest_todo.description as description
      from latest_todo
      """.trimIndent()
    )

    metadata.parameterNames.shouldContainExactly("createdAt")
    metadata.columnAliases.shouldContainExactly("id", "description")
  }

  @Test
  fun `should parse quoted identifiers and function aliases`() {
    val metadata = SqlSelectParser.parse(
      """
      select t."from_id" as "fromId",
             from_unixtime(t.created_at) as `createdAt`,
             count(t.id)
      from todo t
      where t.status = :status
      """.trimIndent()
    )

    metadata.parameterNames.shouldContainExactly("status")
    metadata.columnAliases.shouldContainExactly("fromId", "createdAt", "output1")
  }

  @Test
  fun `should ignore parameters in SQL comments`() {
    val metadata = SqlSelectParser.parse(
      """
      select id as id
      from todo
      -- :ignoredLine
      where id = :id
      /* :ignoredBlock */
      """.trimIndent()
    )

    metadata.parameterNames.shouldContainExactly("id")
    metadata.columnAliases.shouldContainExactly("id")
  }

  @Test
  fun `should reject select all`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("select * from todo where id = :id")
    }

    error.message shouldBe "SELECT * is not allowed. Please specify output columns."
  }

  @Test
  fun `should reject non select statements`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("delete from todo where id = :id")
    }

    error.message shouldBe "SQL API only supports SELECT statements"
  }
}
