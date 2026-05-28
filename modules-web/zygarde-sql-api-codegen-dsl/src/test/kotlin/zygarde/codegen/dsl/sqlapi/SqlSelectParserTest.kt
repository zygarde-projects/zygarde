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
             from_unixtime(t.created_at) as `createdAt`
      from todo t
      where t.status = :status
      """.trimIndent()
    )

    metadata.parameterNames.shouldContainExactly("status")
    metadata.columnAliases.shouldContainExactly("fromId", "createdAt")
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
  fun `should reject unaliased select expressions`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("select count(id) from todo")
    }

    error.message shouldBe "SELECT expression 'count(id)' must declare an AS alias."
  }

  @Test
  fun `should reject arithmetic select expressions without alias`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("select id + 1 from todo")
    }

    error.message shouldBe "SELECT expression 'id + 1' must declare an AS alias."
  }

  @Test
  fun `should reject aliases that cannot be generated as Kotlin properties`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("select id as `bad-name` from todo")
    }

    error.message shouldBe "SQL output alias 'bad-name' is not a valid Kotlin property name."
  }

  @Test
  fun `should reject duplicate output aliases`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("select id as id, description as id from todo")
    }

    error.message shouldBe "SQL output columns contain duplicate aliases: id"
  }

  @Test
  fun `should reject non select statements`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlSelectParser.parse("delete from todo where id = :id")
    }

    error.message shouldBe "SQL API only supports SELECT statements"
  }
}
